# EduCue database migrations

Flyway owns the PostgreSQL schema and Hibernate validates it with
`ddl-auto: validate`. These migrations target a fresh database and represent
the application's current entity model.

The six migrations are applied in this order:

1. `V1__foundation_and_security.sql`
2. `V2__academics_and_admissions.sql`
3. `V3__finance_and_enrollment.sql`
4. `V4__student_completion.sql`
5. `V5__seed_reference_data.sql`
6. `V6__seed_institution_and_staff.sql`

V5 seeds roles, permissions, their mappings, and the initial unit catalogue.
V6 seeds the school profile, Administration, Registrar, Finance, and Human
Health departments, academic years beginning in September from 2020/2021
through 2026/2027, and the initial staff accounts.

Seeded staff emails:

- `admin@educue.local`
- `registrar@educue.local`
- `finance@educue.local`
- `hod@educue.local`
- `trainer@educue.local`

Every seeded account starts with `ChangeMe@123` and must change it after the
first login.

These files replace the previous development history and are intended for a
new database. Never edit one after it has been applied; add a new incremented
migration for subsequent changes.
