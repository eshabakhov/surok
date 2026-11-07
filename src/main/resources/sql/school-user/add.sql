INSERT INTO public.school_user (school_id, user_id)
SELECT school.id, "user".id
FROM public.school, public."user"
WHERE school.id = ? AND NOT school.is_deleted
    AND "user".id = ? AND "user".deleted IS NULL
ON CONFLICT (school_id, user_id) DO UPDATE SET user_id = EXCLUDED.user_id
RETURNING id
