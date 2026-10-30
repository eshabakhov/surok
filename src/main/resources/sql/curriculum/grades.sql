SELECT unnest(
    CASE education_level
        WHEN 'PRIMARY' THEN ARRAY[1, 2, 3, 4]
        WHEN 'BASIC' THEN ARRAY[5, 6, 7, 8, 9]
        WHEN 'SECONDARY' THEN ARRAY[10, 11]
    END
) AS grade
FROM public.federal_curriculum
WHERE id = ? AND NOT is_deleted
ORDER BY grade
