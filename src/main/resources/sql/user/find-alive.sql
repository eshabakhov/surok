SELECT ("user".deleted IS NULL) AS alive
FROM public."user"
WHERE "user".id = ?
