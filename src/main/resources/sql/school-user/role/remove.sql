DELETE FROM public.school_user_role
USING public.school_user
WHERE school_user_role.id = ?
    AND school_user_role.school_user_id = school_user.id
    AND school_user.school_id = ? AND school_user.user_id = ?
