SELECT draft.curriculum_id, schedule.school_id
FROM public.load_draft AS draft
JOIN public.schedule ON schedule.id = draft.schedule_id AND NOT schedule.is_deleted
JOIN public.school ON school.id = schedule.school_id AND NOT school.is_deleted
JOIN public.federal_curriculum AS curriculum
    ON curriculum.id = draft.curriculum_id AND NOT curriculum.is_deleted
WHERE draft.schedule_id = ?
