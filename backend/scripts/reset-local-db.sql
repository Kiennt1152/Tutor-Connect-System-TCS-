-- Reset local dev database after editing V1 (run once manually)
DROP DATABASE IF EXISTS tutorconnectsystem;
CREATE DATABASE tutorconnectsystem
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
