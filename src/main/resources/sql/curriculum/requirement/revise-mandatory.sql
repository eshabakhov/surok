UPDATE public.federal_curriculum_requirement
SET grade = input.grade, subject_id = subject.id, hours = input.hours, updated_at = NOW()
FROM (VALUES (?::integer, ?::bigint, ?::integer, ?::bigint)) AS input(grade, subject, hours, id)
JOIN public.subject ON subject.id = input.subject
    AND NOT subject.is_deleted AND subject.origin = 'FEDERAL'
WHERE federal_curriculum_requirement.id = input.id
    AND federal_curriculum_requirement.part_type = 'MANDATORY'
    AND NOT federal_curriculum_requirement.is_deleted
RETURNING federal_curriculum_requirement.id
