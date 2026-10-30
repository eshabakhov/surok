SELECT *
FROM public.school_class
WHERE school_class.id = ? AND (NOT school_class.is_deleted OR ?)
