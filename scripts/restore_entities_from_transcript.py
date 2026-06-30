#!/usr/bin/env python3
"""Extract Write tool operations from agent transcript JSONL and restore Java files."""

import json
import re
import sys
from pathlib import Path

WORKSPACE = Path(__file__).resolve().parent.parent
TRANSCRIPT = Path(
    r"C:\Users\nguye\.cursor\projects\d-FPTU-Syllabus-FPTU-Semester-9-SU26-SEP490-SE-GRA-ELE-TutorConnectSystem-TCS"
    r"\agent-transcripts\0a5771bb-ff97-4808-9c20-10d08ef21122"
    r"\0a5771bb-ff97-4808-9c20-10d08ef21122.jsonl"
)

JAVA_MARKER = re.compile(r"backend[/\\]src[/\\]main[/\\]java[/\\].*\.java", re.IGNORECASE)
PRIORITY_LINE_RANGES = [(81, 142), (210, 220)]

DELETE_PATHS = [
    "backend/src/main/java/com/tcs/module/finance/entity/Escrow.java",
    "backend/src/main/java/com/tcs/module/finance/enums/EscrowTransactionType.java",
]

SKIP_NAMES = {"Escrow.java", "EscrowTransactionType.java"}


def normalize_java_path(raw_path: str) -> str | None:
    match = JAVA_MARKER.search(raw_path.replace("\\", "/"))
    if not match:
        return None
    rel = match.group(0).replace("\\", "/")
    if rel.split("/")[-1] in SKIP_NAMES:
        return None
    return rel


def in_priority_range(line_no: int) -> bool:
    return any(start <= line_no <= end for start, end in PRIORITY_LINE_RANGES)


def iter_write_ops(transcript_path: Path):
    with transcript_path.open(encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue
            for item in obj.get("message", {}).get("content", []):
                if not isinstance(item, dict):
                    continue
                if item.get("type") != "tool_use" or item.get("name") != "Write":
                    continue
                inp = item.get("input") or {}
                rel = normalize_java_path(inp.get("path", ""))
                contents = inp.get("contents", "")
                if rel and contents:
                    yield line_no, rel, contents


def choose_writes() -> dict[str, str]:
    all_writes: dict[str, tuple[int, str]] = {}
    priority_writes: dict[str, tuple[int, str]] = {}

    for line_no, rel, contents in iter_write_ops(TRANSCRIPT):
        all_writes[rel] = (line_no, contents)
        if in_priority_range(line_no):
            priority_writes[rel] = (line_no, contents)

    final: dict[str, str] = {}
    for rel, (_, contents) in all_writes.items():
        if rel in priority_writes:
            final[rel] = priority_writes[rel][1]
        else:
            final[rel] = contents
    return final


def apply_v5_patches(final: dict[str, str]) -> None:
    tutoring = final.get("backend/src/main/java/com/tcs/module/marketplace/entity/TutoringClass.java")
    if tutoring and "max_sessions" in tutoring:
        tutoring = re.sub(
            r"\n\s*@Column\(name = \"max_sessions\".*?\n\s*private Integer maxSessions = 1;\n",
            "\n",
            tutoring,
            count=1,
        )
        final["backend/src/main/java/com/tcs/module/marketplace/entity/TutoringClass.java"] = tutoring

    tutor_subject = final.get("backend/src/main/java/com/tcs/module/catalog/entity/TutorSubject.java")
    if tutor_subject and "category_id" in tutor_subject:
        tutor_subject = tutor_subject.replace(
            "    @ManyToOne(fetch = FetchType.LAZY)\n"
            "    @JoinColumn(name = \"category_id\")\n"
            "    private Category category;\n\n",
            "",
        )
        tutor_subject = tutor_subject.replace(
            "    @ManyToOne(fetch = FetchType.LAZY)\n"
            "    @JoinColumn(name = \"subject_id\")\n"
            "    private Subject subject;",
            "    @ManyToOne(fetch = FetchType.LAZY, optional = false)\n"
            "    @JoinColumn(name = \"subject_id\", nullable = false)\n"
            "    private Subject subject;",
        )
        final["backend/src/main/java/com/tcs/module/catalog/entity/TutorSubject.java"] = tutor_subject

    escrow_status = final.get("backend/src/main/java/com/tcs/module/finance/enums/EscrowStatus.java")
    if escrow_status and "DISPUTED" not in escrow_status:
        escrow_status = escrow_status.replace(
            "    ON_HOLD\n}",
            "    ON_HOLD,\n    DISPUTED\n}",
        )
        final["backend/src/main/java/com/tcs/module/finance/enums/EscrowStatus.java"] = escrow_status


def main():
    if not TRANSCRIPT.exists():
        print(f"Transcript not found: {TRANSCRIPT}", file=sys.stderr)
        sys.exit(1)

    final = choose_writes()
    apply_v5_patches(final)

    deleted = []
    for rel in DELETE_PATHS:
        target = WORKSPACE / Path(rel)
        if target.exists():
            target.unlink()
            deleted.append(rel)

    written = []
    for rel, contents in sorted(final.items()):
        target = WORKSPACE / Path(rel)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(contents, encoding="utf-8", newline="\n")
        written.append(rel)

    print(f"Deleted {len(deleted)} file(s):")
    for path in deleted:
        print(f"  - {path}")
    print(f"\nWrote {len(written)} file(s):")
    for path in written:
        print(f"  - {path}")


if __name__ == "__main__":
    main()
