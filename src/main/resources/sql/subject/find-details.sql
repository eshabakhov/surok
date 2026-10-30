SELECT *
FROM public.subject
WHERE subject.id = ? AND (NOT subject.is_deleted OR ?)
