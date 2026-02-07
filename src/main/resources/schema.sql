-- 1. Enable pg_trgm extension for efficient text search (fuzzy matching)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Clean up existing tables (Optional: useful for development/reset)
DROP TABLE IF EXISTS subtopic_progress;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS subtopics;
DROP TABLE IF EXISTS topics;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users;

-- 3. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
       email VARCHAR(255) NOT NULL UNIQUE,
       password VARCHAR(255) NOT NULL,
       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Courses Table (ID is String, e.g., 'physics-101')
CREATE TABLE courses (
     id VARCHAR(255) PRIMARY KEY,
     title VARCHAR(255) NOT NULL,
     description TEXT,
     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- Optional for content
     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()  -- Optional for content
);

-- 5. Topics Table
CREATE TABLE topics (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    course_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_topics_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 6. Subtopics Table (Contains Markdown)
CREATE TABLE subtopics (
       id VARCHAR(255) PRIMARY KEY,
       title VARCHAR(255) NOT NULL,
       content TEXT NOT NULL,
       topic_id VARCHAR(255) NOT NULL,
       CONSTRAINT fk_subtopics_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

-- 7. Enrollments Table (Many-to-Many Bridge between User and Course)
CREATE TABLE enrollments (
     id BIGSERIAL PRIMARY KEY,
     user_id BIGINT NOT NULL,
     course_id VARCHAR(255) NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
     CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
     CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
     CONSTRAINT uq_enrollments_user_course UNIQUE (user_id, course_id) -- Prevent duplicate enrollment
);

-- 8. SubtopicRepository Progress Table (Tracks completion)
CREATE TABLE subtopic_progress (
   id BIGSERIAL PRIMARY KEY,
   enrollment_id BIGINT NOT NULL,
   subtopic_id VARCHAR(255) NOT NULL,
   completed BOOLEAN NOT NULL DEFAULT FALSE,
   completed_at TIMESTAMPTZ,
   CONSTRAINT fk_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id) ON DELETE CASCADE,
   CONSTRAINT fk_progress_subtopic FOREIGN KEY (subtopic_id) REFERENCES subtopics(id) ON DELETE CASCADE,
   CONSTRAINT uq_progress_enrollment_subtopic UNIQUE (enrollment_id, subtopic_id) -- Prevent duplicate progress entries
);

-- 9. Indexes for Performance and Search
-- Standard indexes for foreign keys
CREATE INDEX idx_topics_course_id ON topics(course_id);
CREATE INDEX idx_subtopics_topic_id ON subtopics(topic_id);
CREATE INDEX idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX idx_progress_enrollment_id ON subtopic_progress(enrollment_id);

-- GIN Indexes for Case-Insensitive Fuzzy Search (The "Core Feature")
-- This makes queries like "WHERE title ILIKE '%velo%'" extremely fast
CREATE INDEX idx_courses_title_trgm ON courses USING gin (title gin_trgm_ops);
CREATE INDEX idx_courses_desc_trgm ON courses USING gin (description gin_trgm_ops);
CREATE INDEX idx_topics_title_trgm ON topics USING gin (title gin_trgm_ops);
CREATE INDEX idx_subtopics_title_trgm ON subtopics USING gin (title gin_trgm_ops);

-- GIN Index for Content Search
-- Crucial for searching inside large Markdown blocks
CREATE INDEX idx_subtopics_content_trgm ON subtopics USING gin (content gin_trgm_ops);