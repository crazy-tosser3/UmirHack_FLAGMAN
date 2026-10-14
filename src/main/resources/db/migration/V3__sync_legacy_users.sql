ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at timestamp(6) with time zone;
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_id bigint;
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_username varchar(64);
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'users'::regclass AND contype = 'u'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'users'::regclass AND attname = 'telegram_id')]
    ) THEN
        ALTER TABLE users ADD CONSTRAINT ukdus03vmwyluiy7k2p2gcrqbms UNIQUE (telegram_id);
    END IF;
END $$;
