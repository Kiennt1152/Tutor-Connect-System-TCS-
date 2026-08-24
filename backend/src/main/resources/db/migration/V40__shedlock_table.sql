-- ShedLock distributed scheduler lock table
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_shedlock_lock_until ON shedlock(lock_until);
