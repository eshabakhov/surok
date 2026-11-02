SELECT federal_curriculum_requirement.*
FROM public.federal_curriculum_requirement
LEFT JOIN public.subject ON subject.id = federal_curriculum_requirement.subject_id
WHERE {0}
ORDER BY {1}
LIMIT {2}
OFFSET {3}
