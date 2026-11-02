SELECT id FROM public.subject WHERE (school_id = ? OR origin = 'FEDERAL') AND lower(btrim(name)) = lower(btrim(?)) AND is_deleted = ?
