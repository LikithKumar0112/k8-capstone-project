package com.demo.employee.exception;

/**
 * Thrown when attempting to add an employee whose id already exists —
 * preserves the uniqueness rule from the original {@code EmployeeService}.
 */
public class DuplicateEmployeeException extends RuntimeException {

    public DuplicateEmployeeException(int id) {
        super("Employee with id " + id + " already exists");
    }
}
