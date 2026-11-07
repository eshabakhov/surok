SELECT EXISTS (
    SELECT 1 FROM public.school_user
    JOIN public."user" ON "user".id = school_user.user_id
    JOIN public.school ON school.id = school_user.school_id
    WHERE school_user.school_id = ? AND school_user.user_id = ?
        AND "user".deleted IS NULL AND NOT school.is_deleted
)
