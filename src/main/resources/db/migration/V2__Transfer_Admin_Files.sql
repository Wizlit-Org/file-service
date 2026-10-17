-- Transfer file ownership from 'admin' (user_id = 1) to 'comfiapps' (user_id = 2)
UPDATE file SET file_uploader = 2 WHERE file_uploader = 1;
