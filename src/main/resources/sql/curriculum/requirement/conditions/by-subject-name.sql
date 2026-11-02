EXISTS (
    SELECT 1 FROM public.subject
    WHERE subject.id = federal_curriculum_requirement.subject_id
        AND subject.name = ?
        AND subject.is_deleted = false
)
