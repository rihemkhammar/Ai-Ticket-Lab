-- Ensure pgcrypto (UUID generation)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
v_user_id UUID;
BEGIN

    -- 1. Create demo user if not exists
INSERT INTO users (username, email, password_hash, role)
VALUES (
           'demo_technician',
           'tech@demo.com',
           '$2a$12$h6zsu6te5aPTeQWFfweeDOwFYQacw9D0tL5dXAJZvO0D9eu0701pi',
           'TECHNICIAN'
       )
    ON CONFLICT (username) DO NOTHING;

-- 2. Get user id
SELECT id INTO v_user_id
FROM users
WHERE username = 'demo_technician';

-- 3. Insert tickets (idempotent via title uniqueness logic)
IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Conveyor motor overheating') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Conveyor motor overheating',
            'The motor temperature increases after 20 minutes of operation.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Pump vibration detected') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Pump vibration detected',
            'The pump vibrates strongly during normal operation.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Sensor reading unstable') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Sensor reading unstable',
            'The packaging machine sensor gives inconsistent values.',
            'OPEN',
            NOW()
        );
END IF;

END $$;