CREATE TABLE member_profiles (
                                 id VARCHAR(255) PRIMARY KEY,
                                 email VARCHAR(255),
                                 first_name VARCHAR(100),
                                 last_name VARCHAR(100),
                                 phone VARCHAR(30),
                                 member_type VARCHAR(50),
                                 member_code VARCHAR(50),
                                 borrowing_limit INTEGER NOT NULL,
                                 loan_period_days INTEGER NOT NULL,
                                 outstanding_balance NUMERIC(12, 2),
                                 avatar_key VARCHAR(512),
                                 created_at TIMESTAMPTZ,
                                 updated_at TIMESTAMPTZ
);


CREATE UNIQUE INDEX IF NOT EXISTS ux_member_profiles_email
    ON member_profiles (email)
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_member_profiles_member_code
    ON member_profiles (member_code)
    WHERE member_code IS NOT NULL;
