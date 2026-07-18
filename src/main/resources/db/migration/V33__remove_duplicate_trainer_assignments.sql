-- Preserve any legacy allocations by translating their calendar dates to the
-- institution academic years that contain them. A row without an end date
-- remains open-ended. Rows created after V22 may already exist in the target,
-- so this migration only copies the legacy trainer table once before dropping it.
INSERT INTO lecturer_unit_assignments (
    uuid, course_unit_placement_id, lecturer_id,
    effective_from_academic_year_id, effective_to_academic_year_id,
    start_year, assigned_at, assigned_by, enabled, version, updated_at
)
SELECT gen_random_uuid(), legacy.course_unit_placement_id, legacy.trainer_id,
       from_year.id, to_year.id, from_year.start_year,
       legacy.assigned_at, legacy.assigned_by,
       CASE WHEN legacy.effective_to IS NULL OR legacy.effective_to >= CURRENT_DATE
            THEN TRUE ELSE FALSE END,
       0, CURRENT_TIMESTAMP
FROM trainer_assignments legacy
JOIN LATERAL (
    SELECT year.id, year.start_year
    FROM academic_years year
    WHERE legacy.effective_from BETWEEN year.start_date AND year.end_date
    ORDER BY year.start_date DESC
    LIMIT 1
) from_year ON TRUE
LEFT JOIN LATERAL (
    SELECT year.id
    FROM academic_years year
    WHERE legacy.effective_to IS NOT NULL
      AND legacy.effective_to BETWEEN year.start_date AND year.end_date
    ORDER BY year.start_date DESC
    LIMIT 1
) to_year ON TRUE;

-- LecturerUnitAssignment is now the single source of truth. The legacy table
-- represented the same relationship using calendar dates.
DROP TABLE IF EXISTS trainer_assignments;
