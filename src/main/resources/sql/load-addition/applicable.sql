SELECT DISTINCT school_class.grade
FROM public.school_class
JOIN public.schedule ON schedule.school_id = school_class.school_id
WHERE schedule.id = ? AND NOT school_class.is_deleted
    AND school_class.grade = ANY(?::integer[])
ORDER BY school_class.grade
