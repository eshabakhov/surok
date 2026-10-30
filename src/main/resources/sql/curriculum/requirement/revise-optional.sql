UPDATE public.federal_curriculum_requirement
SET grade = ?, hours = ?, updated_at = NOW()
WHERE id = ? AND part_type = 'OPTIONAL' AND NOT is_deleted
RETURNING id
