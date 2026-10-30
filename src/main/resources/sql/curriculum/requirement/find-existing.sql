SELECT requirement.id
FROM public.federal_curriculum_requirement AS requirement
LEFT JOIN public.subject ON subject.id = requirement.subject_id
WHERE requirement.federal_curriculum_id = ? AND requirement.grade = ?
    AND (subject.id = ?::bigint OR requirement.part_type = 'OPTIONAL')
    AND requirement.part_type = CAST(? AS curriculum_part_type) AND requirement.is_deleted = ?
