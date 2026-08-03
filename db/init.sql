-- ---------------------------------------------------------------------------
-- Employee Management — database bootstrap (MySQL 8.0)
--
-- The "DB" half of the developer hand-off. The Spring Boot backend runs with
-- JPA `ddl-auto=update`, so it will create the `employees` table on first boot;
-- this script exists to (a) document the schema and (b) seed sample rows so the
-- UI has data the moment you start it locally.
--
-- Run it against a MySQL you started on localhost, e.g.:
--   mysql -u root -p < db/init.sql
-- ---------------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS employeedb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Application user the backend authenticates as (matches application.yml defaults).
CREATE USER IF NOT EXISTS 'employee'@'%' IDENTIFIED BY 'employeepass';
GRANT ALL PRIVILEGES ON employeedb.* TO 'employee'@'%';
FLUSH PRIVILEGES;

USE employeedb;

-- Schema mirrors the JPA @Entity com.demo.employee.model.Employee.
CREATE TABLE IF NOT EXISTS employees (
    id         INT            NOT NULL,          -- client-supplied, must be positive & unique
    name       VARCHAR(255)   NOT NULL,
    department VARCHAR(255)   NOT NULL,
    salary     DOUBLE         NOT NULL,
    PRIMARY KEY (id)
);

-- Sample data (idempotent-ish: ignores rows whose id already exists).
INSERT IGNORE INTO employees (id, name, department, salary) VALUES
    (1, 'Asha Rao',      'Engineering', 95000),
    (2, 'Vikram Singh',  'Finance',     82000),
    (3, 'Meera Nair',    'HR',          68000),
    (4, 'Daniel Cruz',   'Engineering', 91000);
