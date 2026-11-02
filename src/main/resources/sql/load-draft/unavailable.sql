SELECT requirement.id FROM public.load_draft AS draft
JOIN public.schedule ON schedule.id = draft.schedule_id
JOIN public.federal_curriculum_requirement AS requirement
    ON requirement.federal_curriculum_id = draft.curriculum_id
LEFT JOIN public.subject ON subject.id = requirement.subject_id AND NOT subject.is_deleted
WHERE draft.schedule_id = ? AND requirement.part_type = 'MANDATORY'
    AND NOT requirement.is_deleted AND requirement.grade = ANY(?::integer[])
    AND EXISTS (SELECT 1 FROM public.school_class AS clazz
        WHERE clazz.school_id = schedule.school_id AND clazz.grade = requirement.grade
            AND NOT clazz.is_deleted)
    AND (subject.id IS NULL OR subject.origin != 'FEDERAL')
ORDER BY requirement.grade, requirement.id
