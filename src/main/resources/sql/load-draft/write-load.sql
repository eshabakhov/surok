INSERT INTO public.class_curriculum
    (schedule_id, school_class_id, subject_id, hours)
VALUES (?, ?, ?, ?)
ON CONFLICT (schedule_id, school_class_id, subject_id) DO UPDATE
SET hours = EXCLUDED.hours
WHERE class_curriculum.hours IS DISTINCT FROM EXCLUDED.hours
