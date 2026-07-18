# EduCue production baseline

This directory is the squashed Flyway history for a brand-new production database.
It represents the final schema after historical migrations V1–V40.

Run the application with the `production` Spring profile. That profile points Flyway
to `classpath:db/production`; it must never be combined with `classpath:db/migration`.

Migration order:

1. Institution and academic-year foundations
2. Identity, roles, permissions, and users
3. Academic courses, periods, units, placements, and lecturer assignments
4. Admissions and intakes
5. Students, enrollments, unit registrations, and exam cards
6. Results
7. Finance, ledger, PayBill, and STK Push
8. Cross-category constraints and indexes
9. Roles and permission reference data

The baseline contains no institution, department, course, unit, student, payment,
or staff-user demonstration data. The first administrator is created at startup from
the required `BOOTSTRAP_ADMIN_*` environment variables and receives a BCrypt hash.

Do not use this baseline against a database that already has the historical Flyway
history. Existing installations must continue using `db/migration`. Never edit these
files after the first production deployment; add new migrations starting at V10.
