CREATE DATABASE demodb;
\c demodb;

CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    company TEXT NOT NULL
);