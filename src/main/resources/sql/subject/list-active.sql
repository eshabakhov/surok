SELECT id FROM public.subject WHERE (school_id = ? OR origin = 'FEDERAL') AND is_deleted = ? ORDER BY name, id
