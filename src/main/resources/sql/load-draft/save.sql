INSERT INTO public.load_draft (schedule_id, curriculum_id)
SELECT ?, id FROM public.federal_curriculum WHERE id = ? AND NOT is_deleted
ON CONFLICT (schedule_id) DO UPDATE
SET curriculum_id = EXCLUDED.curriculum_id, updated_at = clock_timestamp()
