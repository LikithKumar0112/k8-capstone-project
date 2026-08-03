package com.demo.employee.exception;

/**
 * Thrown when an employee lookup or removal targets an id that does not exist.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(int id) {
        super("No employee found with id " + id);
    }
}
