package com.tcs.module.identity.enums;

/**
 * Repurposed for tutor verification use case (as of v1.1).
 *
 * <p>Originally a generic 3-bucket taxonomy for tutor credentials, but
 * since most tutors are university students, the platform no longer
 * requires graduation/teaching certificates up-front. The three buckets
 * are now mapped to the actual two-sided CCCD/CMND plus optional extras:
 *
 * <ul>
 *   <li>{@link #ID_CARD} — CCCD/CMND mặt trước (ID front side, required)</li>
 *   <li>{@link #DEGREE} — CCCD/CMND mặt sau (ID back side, required).
 *       Field name kept for DB compatibility; not used as an academic degree.</li>
 *   <li>{@link #CERTIFICATE} — Other credentials (optional, multi-upload).
 *       Includes graduation certificates, teaching certificates,
 *       IELTS/TOEFL scores, etc.</li>
 * </ul>
 *
 * <p>Renaming the enum values would require a PostgreSQL enum migration;
 * reusing the values keeps the existing CHECK constraint and existing
 * rows intact.
 */
public enum VerificationDocumentType {
    ID_CARD,
    DEGREE,
    CERTIFICATE,
    LICENSE
}