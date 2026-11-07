INSERT INTO public."user" (username, password, email, corporate)
VALUES (?, ?, ?, ?)
RETURNING *
