INSERT INTO public.subject (origin, name) VALUES ('FEDERAL', ?)
ON CONFLICT (lower(btrim(name))) WHERE origin = 'FEDERAL' AND is_deleted = false DO NOTHING
RETURNING id
