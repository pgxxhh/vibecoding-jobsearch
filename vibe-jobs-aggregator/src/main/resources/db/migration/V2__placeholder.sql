-- Placeholder migration to maintain version continuity
-- This migration was added to fix the missing V2 version that was causing
-- Flyway to stop at V3 and not execute V4 (job_data_source tables) and V7 (crawler tables)

-- No schema changes needed in this version
SELECT 1 as placeholder_migration_v2;
