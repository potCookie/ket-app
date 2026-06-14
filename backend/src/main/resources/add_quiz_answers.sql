-- Add quiz_answers column to store user's answer selections
ALTER TABLE study_log ADD COLUMN IF NOT EXISTS quiz_answers TEXT COMMENT 'JSON: user selected answer indices, e.g. {"0":0,"1":2}';
