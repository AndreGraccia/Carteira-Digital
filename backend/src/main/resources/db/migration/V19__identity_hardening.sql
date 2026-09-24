-- V2/V4 came from the e-wallet template: three demo users whose hashes are public, two of them ADMIN,
-- created in every environment including production. An admin lists every user's wallets and transactions.
-- Disable them without deleting anything: a value that is not a BCrypt hash never matches a password,
-- and the admin role goes away. To have an admin, promote a real account by hand (see guia-do-desenvolvedor).
-- Reverting means restoring the hashes from V2 and the rows from V4.
UPDATE public."user"
SET password = '{disabled}'
WHERE id IN (1, 2, 3)
  AND username IN ('johndoe', 'lindacalvin', 'jeffreytaylor');

DELETE FROM public.user_role
WHERE role_id = (SELECT id FROM public.role WHERE type = 'ROLE_ADMIN')
  AND user_id IN (SELECT id FROM public."user" WHERE username IN ('johndoe', 'lindacalvin', 'jeffreytaylor'));

-- Login and sign-up compare ignoring case; the database must agree, or "Gui" and "gui" can coexist
-- and the login lookup finds two rows. If this fails, the error names the duplicate to resolve first.
CREATE UNIQUE INDEX uq_user_username_lower ON public."user" (lower(username));
CREATE UNIQUE INDEX uq_user_email_lower ON public."user" (lower(email));
