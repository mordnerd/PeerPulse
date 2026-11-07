CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     full_name TEXT NOT NULL,
                                     role TEXT NOT NULL CHECK (role IN ('MENTEE','MENTOR','ADMIN')),
    status TEXT NOT NULL CHECK (status IN ('PENDING','ACTIVE','REJECTED'))
    );

CREATE TABLE IF NOT EXISTS profiles (
                                        user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    academic_year TEXT,
    major TEXT,
    skills TEXT,
    certifications TEXT,
    transcript TEXT
    );

CREATE TABLE IF NOT EXISTS courses (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       code TEXT NOT NULL UNIQUE,
                                       title TEXT NOT NULL,
                                       description TEXT
);

CREATE TABLE IF NOT EXISTS mentor_courses (
                                              mentor_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    PRIMARY KEY (mentor_id, course_id)
    );

CREATE TABLE IF NOT EXISTS sessions (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        mentor_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id INTEGER REFERENCES courses(id),
    start_time TEXT NOT NULL,
    duration_min INTEGER NOT NULL DEFAULT 60,
    location TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN'
    );

CREATE TABLE IF NOT EXISTS bookings (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        session_id INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    mentee_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'BOOKED',
    UNIQUE (session_id, mentee_id)
    );

CREATE TABLE IF NOT EXISTS feedback (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        booking_id INTEGER NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    comment TEXT NOT NULL,
    created_at TEXT NOT NULL
    );

CREATE TABLE IF NOT EXISTS reports (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       reporter_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    created_at TEXT NOT NULL,
    resolved INTEGER NOT NULL DEFAULT 0
    )