CREATE TABLE IF NOT EXISTS employees (
    id         INT            NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    department VARCHAR(255)   NOT NULL,
    salary     DOUBLE         NOT NULL,
    PRIMARY KEY (id)
);

INSERT IGNORE INTO employees (id, name, department, salary) VALUES
    (1, 'Asha Rao',     'Engineering', 95000),
    (2, 'Vikram Singh', 'Finance',     82000),
    (3, 'Meera Nair',   'HR',          68000),
    (4, 'Daniel Cruz',  'Engineering', 91000);
