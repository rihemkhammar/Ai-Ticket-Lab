-- Seed data for local dev/test only. Must NOT run in the prod profile.
-- Reuses the existing demo_technician user created in V3__seed_tickets.sql.

DO $$
DECLARE
v_user_id UUID;
BEGIN

SELECT id INTO v_user_id
FROM users
WHERE username = 'demo_technician';

IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Conveyor belt motor overheating') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Conveyor belt motor overheating',
            'Motor temperature exceeds 90C on line 3, smoke smell reported by operator. Risk of fire and equipment damage if not addressed today.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Hydraulic press pressure leak') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Hydraulic press pressure leak',
            'Slow hydraulic fluid leak detected near press #7 seals. Pressure drop of ~5% observed over the last shift.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Warehouse HVAC filter clogged') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Warehouse HVAC filter clogged',
            'Air filter in zone B HVAC unit appears clogged, reduced airflow reported, no immediate safety impact.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Loading dock light flickering') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Loading dock light flickering',
            'Light fixture near dock door 2 flickers intermittently. Cosmetic/nuisance issue, no equipment affected.',
            'OPEN',
            NOW()
        );
END IF;

    IF NOT EXISTS (SELECT 1 FROM tickets WHERE title = 'Packaging line label printer jam') THEN
        INSERT INTO tickets (created_by, title, description, status, created_at)
        VALUES (
            v_user_id,
            'Packaging line label printer jam',
            'Label printer on packaging line 1 jams occasionally, operator can clear manually, minor throughput impact.',
            'OPEN',
            NOW()
        );
END IF;

END $$;