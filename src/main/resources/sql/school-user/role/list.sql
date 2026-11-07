SELECT school_user_role.id FROM public.school_user_role
JOIN public.school_user ON school_user.id = school_user_role.school_user_id
WHERE school_user.school_id = ? AND school_user.user_id = ?
ORDER BY school_user_role.id
