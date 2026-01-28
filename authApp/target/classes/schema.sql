-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
       id character varying(32) PRIMARY KEY,
       username character varying(100) NOT NULL UNIQUE,
       password character varying(255) NOT NULL
);

-- Связующая таблица user_roles
CREATE TABLE IF NOT EXISTS user_roles (
        user_id character varying(32),
        role character varying(10),
        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Таблица сессий
CREATE TABLE IF NOT EXISTS sessions (
        session_id character varying(32) PRIMARY KEY,
        token character varying(64) NOT NULL UNIQUE,
        expiration timestamp,
        user_id character varying(32),
        FOREIGN KEY (user_id) REFERENCES users(id)
)