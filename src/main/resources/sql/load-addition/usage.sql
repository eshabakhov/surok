SELECT clazz.id AS school_class_id,
    COALESCE(SUM(actual.hours), 0)::integer AS current,
    COALESCE(SUM(COALESCE(addition.hours, actual.hours, 0)), 0)::integer
        AS planned
FROM public.schedule
JOIN public.school_class AS clazz ON clazz.school_id = schedule.school_id
    AND NOT clazz.is_deleted AND clazz.grade = ?
LEFT JOIN public.subject ON subject.school_id = schedule.school_id AND subject.origin = 'SCHOOL'
LEFT JOIN public.class_curriculum AS actual ON actual.schedule_id = schedule.id
    AND actual.school_class_id = clazz.id AND actual.subject_id = subject.id
LEFT JOIN public.load_draft_addition AS addition ON addition.schedule_id = schedule.id
    AND addition.grade = clazz.grade AND addition.subject_id = subject.id
WHERE schedule.id = ?
GROUP BY clazz.id
ORDER BY clazz.id
