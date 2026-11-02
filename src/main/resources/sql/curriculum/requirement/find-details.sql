SELECT requirement.*
FROM public.federal_curriculum_requirement AS requirement
JOIN public.federal_curriculum AS curriculum ON curriculum.id = requirement.federal_curriculum_id
WHERE requirement.id = ? AND requirement.part_type = CAST(? AS curriculum_part_type)
    AND NOT requirement.is_deleted AND NOT curriculum.is_deleted
