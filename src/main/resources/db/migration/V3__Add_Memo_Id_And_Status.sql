-- Add memo_id and status to file table
ALTER TABLE file
    ADD COLUMN IF NOT EXISTS memo_id BIGINT,
    ADD COLUMN IF NOT EXISTS status SMALLINT NOT NULL DEFAULT 0;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_file_memo_id ON file (memo_id);
CREATE INDEX IF NOT EXISTS idx_file_status_created ON file (status, file_created_timestamp);
