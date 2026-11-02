SELECT count(*)
FROM public.federal_curriculum_requirement
LEFT JOIN public.subject ON subject.id = federal_curriculum_requirement.subject_id
WHERE {0}
