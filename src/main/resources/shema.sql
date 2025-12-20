-- Создание таблицы пользователей
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(512) NOT NULL UNIQUE
);

-- Создание таблицы товаров
CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    available BOOLEAN NOT NULL,
    owner_id BIGINT NOT NULL,
    request_id BIGINT,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Создание таблицы бронирований
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    item_id BIGINT NOT NULL,
    booker_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (booker_id) REFERENCES users(id)
);

-- Создание таблицы комментариев
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text TEXT NOT NULL,
    item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    created DATETIME NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
);
