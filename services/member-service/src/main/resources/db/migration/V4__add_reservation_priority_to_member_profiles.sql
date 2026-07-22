-- Adds reservation_priority to member_profiles.
--
-- Existing profiles (seeded and JIT-created) get the default 0; the
-- onboarding endpoint (PATCH /api/v1/members/me/role) overwrites this with
-- the value pulled from the selected Keycloak realm role's attributes,
-- alongside borrowing_limit and loan_period_days.
ALTER TABLE member_profiles
    ADD COLUMN IF NOT EXISTS reservation_priority INTEGER NOT NULL DEFAULT 0;