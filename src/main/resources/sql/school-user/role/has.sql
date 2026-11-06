SELECT EXISTS (
    SELECT 1 FROM public.school_user_role
    JOIN public.school_user ON school_user.id = school_user_role.school_user_id
    JOIN public.role ON role.id = school_user_role.role_id
    WHERE school_user.school_id = ? AND school_user.user_id = ?
        AND role.name = CAST(? AS role_type)
)
