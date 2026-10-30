SELECT id FROM public.class_curriculum
WHERE schedule_id = ?
ORDER BY id
FOR UPDATE
