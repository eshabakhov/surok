SELECT clazz.id AS school_class_id, subject.id AS subject_id,
    COALESCE(actual.hours, 0) AS current,
    COALESCE(required.hours, 0) AS proposed,
    addition.hours AS planned
FROM public.load_draft AS draft
JOIN public.schedule ON schedule.id = draft.schedule_id
JOIN public.school_class AS clazz ON clazz.school_id = schedule.school_id
    AND NOT clazz.is_deleted AND clazz.grade = ANY(?::integer[])
JOIN public.subject ON (subject.origin = 'FEDERAL' OR subject.school_id = schedule.school_id)
LEFT JOIN public.class_curriculum AS actual ON actual.schedule_id = schedule.id
    AND actual.school_class_id = clazz.id AND actual.subject_id = subject.id
LEFT JOIN LATERAL (
    SELECT SUM(requirement.hours)::integer AS hours
    FROM public.federal_curriculum_requirement AS requirement
    WHERE requirement.federal_curriculum_id = draft.curriculum_id
        AND requirement.grade = clazz.grade AND requirement.subject_id = subject.id
        AND requirement.part_type = 'MANDATORY' AND NOT requirement.is_deleted
        AND subject.origin = 'FEDERAL' AND NOT subject.is_deleted
) AS required ON true
LEFT JOIN public.load_draft_addition AS addition ON addition.schedule_id = schedule.id
    AND addition.grade = clazz.grade AND addition.subject_id = subject.id
WHERE draft.schedule_id = ?
    AND (NOT subject.is_deleted OR actual.id IS NOT NULL OR addition.subject_id IS NOT NULL)
ORDER BY clazz.grade, clazz.litera, clazz.id, subject.name, subject.id
