# Data, Storage and Security Consistency Review

Date: 12/08/2026
Repository baseline: `https://github.com/Kiennt1152/Tutor-Connect-System-TCS-`

## Summary

The reports were aligned to the current codebase instead of treating planned items as implemented work. The repository baseline is Java 21, Spring Boot 4.0.6, React 19.2.6, TypeScript 6.0.2, Vite 8.0.12, MySQL 8, Flyway V26, Docker Compose, Nginx, Spring Security JWT/BCrypt and Spring WebSocket/STOMP.

## Main inconsistencies fixed or marked

| Area | Previous issue | Corrected baseline |
|---|---|---|
| Conceptual vs physical data model | Report 3 listed only 24 entities and Report 4 described an incomplete physical schema. | Report 3 now states those are core aggregates; Report 4 states the physical baseline has 79 JPA entities and Flyway V1-V26. |
| Verification status | Some use-case text implied Tutor/TutorCenter owns `verification_status`. | VerificationRequest and VerificationHistory are authoritative; profile tables do not own that field. |
| File storage | Older wording exposed `/uploads/**` or relied on content type only. | Only `/uploads/public/**` is static; sensitive files use `/api/files/private/{fileId}`; upload validation includes magic bytes. |
| Excel data exchange | Report 1/3 described bulk Excel as current capability. | Current release supports manual roster/class data management; Excel import/export is deferred until implemented. |
| Deployment | Some documents implied AWS or unimplemented infrastructure. | Current baseline is Docker Compose single-host; AWS is a target option. |
| Security requirements | Failed-login lockout and idle timeout were written like satisfied controls. | They are documented as gaps; current implementation uses JWT 24h and no password failed-attempt counter. |
| Webhook/API security | API key/signature/rate limiting was described ahead of implementation. | Public webhook and AI endpoints are identified as production-hardening gaps. |
| WebSocket security | Origin policy was not called out. | STOMP auth/participant checks are implemented; production WebSocket origin allow-list is still needed. |

## Files updated

- `Report_1_VS.docx`
- `Report_2.0_ProjectPlan.docx`
- `Report_2.1_ProjectTracking.xlsx`
- `Report_3.0_PRD.docx`
- `Report_3.1_UCS (1).docx`
- `Report_4.0_TDS.docx`
- `Report_4.1_FDS_Template.docx`
- `Report_5.1_UnitTest.xlsx`
- `Report_5.2_IntegrationTest.xlsx`
- `Report_5.3_SystemTest.xlsx`
- `Report5.4_User_Acceptance_Test_FULL.xlsx`
- `Copy of Report 6.1_DeploymentGuide_Template.docx`
- `Copy of Report 6.2_UserManual_Template.docx`

`Report_5.0_TestDoc.docx` was not present in `docs` at the time of update, so it was not modified.
