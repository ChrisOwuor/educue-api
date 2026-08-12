# EduCue production baseline

Flyway owns the PostgreSQL schema and Hibernate validates it with
`ddl-auto: validate`. This seven-file baseline is only for a fresh database.

Schema and reference migrations:

1. `V1__foundation_and_security.sql`
2. `V2__academics_and_admissions.sql`
3. `V3__finance_and_enrollment.sql`
4. `V4__student_completion.sql`
5. `V5__seed_reference_data.sql`

User seeders:

6. `V6__seed_core_users.sql`
7. `V7__seed_academic_users.sql`

All users, including administrators, registrars and finance staff, belong to
an explicit department. The bootstrap password is `ChangeMe@123`; every seeded
account must change it after first login.

Unit codes use a subject prefix and a stage suffix. The suffix maps directly
to the course period: `101/102`, `201/202`, `301/302`, `401/402`, `501/502`,
and `601/602`. Examples are `NUR 101`, `BIO 202` and `COMP 102`.

Do not install this replacement history over a database that already contains
the previous Flyway history. It is valid because the target database was
explicitly cleared. Future changes must be new incremented migrations.
