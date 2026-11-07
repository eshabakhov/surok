SELECT "user".id FROM public.school_user
JOIN public."user" ON "user".id = school_user.user_id
WHERE school_user.school_id = ? AND "user".deleted IS NULL
ORDER BY "user".id
