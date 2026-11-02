SELECT school_class.id
FROM public.schedule
JOIN public.school_class ON school_class.school_id = schedule.school_id
JOIN public.subject ON (subject.origin = 'FEDERAL' OR subject.school_id = schedule.school_id)
WHERE schedule.id = ? AND school_class.id = ? AND subject.id = ?
    AND NOT schedule.is_deleted AND NOT school_class.is_deleted
    AND (NOT subject.is_deleted OR ? = 0 OR EXISTS (
        SELECT 1 FROM public.class_curriculum AS load
        WHERE load.schedule_id = schedule.id AND load.school_class_id = school_class.id
            AND load.subject_id = subject.id
    ))
FOR SHARE OF school_class, subject
