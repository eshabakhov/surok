WITH input AS (
    SELECT CAST(? AS integer) AS grade, CAST(? AS integer) AS hours
)
INSERT INTO public.load_draft_addition (grade, hours, schedule_id, subject_id)
SELECT input.grade, input.hours, schedule.id, subject.id
FROM input CROSS JOIN public.schedule
JOIN public.subject ON subject.school_id = schedule.school_id AND subject.origin = 'SCHOOL'
WHERE schedule.id = ? AND NOT schedule.is_deleted AND subject.id = ?
    AND (NOT subject.is_deleted OR (input.hours = 0 AND EXISTS (
        SELECT 1 FROM public.load_draft_addition AS previous
        WHERE previous.schedule_id = schedule.id AND previous.subject_id = subject.id
            AND previous.grade = input.grade
    )))
ON CONFLICT (schedule_id, grade, subject_id)
DO UPDATE SET hours = EXCLUDED.hours
