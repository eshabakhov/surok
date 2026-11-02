SELECT requirement.*
FROM public.federal_curriculum_requirement AS requirement
JOIN public.federal_curriculum AS curriculum ON curriculum.id = requirement.federal_curriculum_id
WHERE curriculum.id = ? AND requirement.grade = ? AND requirement.part_type = 'OPTIONAL'
    AND NOT curriculum.is_deleted AND NOT requirement.is_deleted
