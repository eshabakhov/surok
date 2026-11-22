DELETE FROM public.user_role
WHERE user_id = ? AND role_id IN (
    SELECT id FROM public.role
    WHERE name IN ('BASIC_MAKER', 'ADVANCED_MAKER', 'PRO_MAKER')
)
