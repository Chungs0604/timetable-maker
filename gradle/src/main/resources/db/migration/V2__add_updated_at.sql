-- users.updated_at 컬럼 추가 + 기본값 now
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- created_at에도 기본값 보강(없다면)
ALTER TABLE users
  ALTER COLUMN created_at SET DEFAULT NOW();

-- 혹시 기존 데이터에 null이 있으면 채우기(안전)
UPDATE users SET created_at = NOW() WHERE created_at IS NULL;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;
