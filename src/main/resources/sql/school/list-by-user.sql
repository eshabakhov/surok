SELECT school.id FROM public.school
JOIN public.school_user ON school_user.school_id = school.id
WHERE school_user.user_id = ? AND NOT school.is_deleted
ORDER BY school.id
