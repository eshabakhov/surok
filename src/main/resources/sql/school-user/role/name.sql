SELECT role.name FROM public.school_user_role
JOIN public.role ON role.id = school_user_role.role_id
WHERE school_user_role.id = ?
