CREATE TABLE IF NOT EXISTS Products(
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40)    NOT NULL UNIQUE,
    description TEXT,
    category      VARCHAR(80) NOT NULL,
    stock         INT DEFAULT 0,
    unit_price DECIMAL(7,2) NOT NULL DEFAULT 0.99,
    created       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    visibility BOOL default 0,
    check (stock >= 0),
    check (Unit_price >= 0)
)