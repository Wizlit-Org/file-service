-- Create function to generate random file ID
CREATE OR REPLACE FUNCTION generate_file_id() RETURNS VARCHAR(32) AS $$
DECLARE
    chars TEXT := 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    result VARCHAR(32) := '';
    i INTEGER := 0;
BEGIN
    -- Generate 32 character random string
    FOR i IN 1..32 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::integer, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Create file table
CREATE TABLE file (
    file_id VARCHAR(32) PRIMARY KEY DEFAULT generate_file_id(),
    file_size BIGINT NOT NULL,
    file_uploader BIGINT NOT NULL,
    file_created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_type VARCHAR(50) NOT NULL,
    file_extension VARCHAR(15) NOT NULL,
    file_hash VARCHAR(32) NOT NULL UNIQUE
);

-- Create file_usage table with composite primary key
CREATE TABLE view (
    file_id VARCHAR(32) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    last_viewed_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (file_id),
    FOREIGN KEY (file_id) REFERENCES file(file_id) ON DELETE CASCADE
);