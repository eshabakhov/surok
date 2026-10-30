SELECT schedule.school_id
FROM public.schedule
JOIN public.school ON school.id = schedule.school_id AND NOT school.is_deleted
WHERE schedule.id = ? AND NOT schedule.is_deleted
FOR UPDATE OF schedule
