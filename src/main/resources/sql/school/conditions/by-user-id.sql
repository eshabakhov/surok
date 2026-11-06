EXISTS (
    SELECT 1 FROM public.school_user
    WHERE school_user.school_id = school.id AND school_user.user_id = ?
)
