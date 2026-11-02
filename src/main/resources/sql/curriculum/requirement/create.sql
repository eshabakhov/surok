INSERT INTO public.federal_curriculum_requirement (federal_curriculum_id, grade,
subject_id, hours, part_type, is_deleted)
SELECT input.curriculum, input.grade, subject.id, input.hours,
    CAST(input.part AS curriculum_part_type), input.deleted
FROM (VALUES (?::bigint, ?::integer, ?::bigint, ?::integer, ?::text, ?::boolean))
    AS input(curriculum, grade, subject, hours, part, deleted)
LEFT JOIN public.subject ON subject.id = input.subject
    AND NOT subject.is_deleted AND subject.origin = 'FEDERAL'
WHERE input.part = 'OPTIONAL' OR subject.id IS NOT NULL
RETURNING federal_curriculum_requirement.*
