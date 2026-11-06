INSERT INTO public.school_user_role (school_user_id, role_id)
SELECT school_user.id, role.id FROM public.school_user, public.role
WHERE school_user.school_id = ? AND school_user.user_id = ?
    AND role.name = CAST(? AS role_type)
ON CONFLICT (school_user_id, role_id) DO UPDATE SET role_id = EXCLUDED.role_id
RETURNING id
