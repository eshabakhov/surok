SELECT other.id FROM public.subject AS current JOIN public.subject AS other
    ON (other.origin = 'FEDERAL' OR other.school_id = current.school_id)
WHERE current.id = ? AND lower(btrim(other.name)) = lower(btrim(?)) AND other.is_deleted = ?
    AND other.id != current.id
