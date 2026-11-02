SELECT addition.subject_id, addition.hours,
    (subject.origin = 'SCHOOL' AND subject.school_id = schedule.school_id) AS owned,
    NOT subject.is_deleted AS available
FROM public.load_draft_addition AS addition
JOIN public.schedule ON schedule.id = addition.schedule_id
JOIN public.subject ON subject.id = addition.subject_id
WHERE addition.schedule_id = ? AND addition.grade = ?
ORDER BY addition.subject_id
