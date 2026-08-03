package com.demo.employee.service;

import com.demo.employee.exception.DuplicateEmployeeException;
import com.demo.employee.exception.EmployeeNotFoundException;
import com.demo.employee.model.Employee;
import com.demo.employee.repository.EmployeeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for managing {@link Employee} records.
 *
 * <p>Faithful to the original Jenkins-capstone {@code EmployeeService} rules —
 * positive ids, uniqueness on add, validation and structured logging — but now
 * persisting to MySQL via {@link EmployeeRepository} and caching reads in Redis
 * through Spring's cache abstraction.</p>
 *
 * <ul>
 *   <li>{@code employees} cache → result of {@link #getAllEmployees()}</li>
 *   <li>{@code employee} cache → individual {@link #findEmployeeById(int)} results</li>
 * </ul>
 * Mutations ({@link #addEmployee}, {@link #removeEmployee}) evict both caches so
 * readers never see stale data.
 */
@Service
public class EmployeeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    /**
     * Adds a new employee.
     *
     * @throws IllegalArgumentException     if the employee is {@code null} or its id is not positive
     * @throws DuplicateEmployeeException   if an employee with the same id already exists
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "employees", allEntries = true),
            @CacheEvict(cacheNames = "employee", key = "#employee.id")
    })
    @Transactional
    public Employee addEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee must not be null");
        }
        if (employee.getId() <= 0) {
            throw new IllegalArgumentException("Employee id must be a positive value");
        }
        if (repository.existsById(employee.getId())) {
            throw new DuplicateEmployeeException(employee.getId());
        }
        Employee saved = repository.save(employee);
        LOGGER.info("Added employee with id {} and name {}", saved.getId(), saved.getName());
        return saved;
    }

    /**
     * Removes the employee matching the supplied id.
     *
     * @throws IllegalArgumentException   if the id is not positive
     * @throws EmployeeNotFoundException  if no employee has that id
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "employees", allEntries = true),
            @CacheEvict(cacheNames = "employee", key = "#id")
    })
    @Transactional
    public void removeEmployee(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be a positive value");
        }
        if (!repository.existsById(id)) {
            LOGGER.warn("No employee found to remove for id {}", id);
            throw new EmployeeNotFoundException(id);
        }
        repository.deleteById(id);
        LOGGER.info("Removed employee with id {}", id);
    }

    /**
     * Finds an employee by id, served from the {@code employee} cache when warm.
     *
     * @throws IllegalArgumentException   if the id is not positive
     * @throws EmployeeNotFoundException  if no employee has that id
     */
    @Cacheable(cacheNames = "employee", key = "#id")
    @Transactional(readOnly = true)
    public Employee findEmployeeById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee id must be a positive value");
        }
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
        LOGGER.debug("Lookup for employee id {} succeeded", id);
        return employee;
    }

    /**
     * Returns all employees, served from the {@code employees} cache when warm.
     */
    @Cacheable(cacheNames = "employees")
    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        List<Employee> all = repository.findAll();
        LOGGER.debug("Returning all {} employees", all.size());
        return all;
    }
}
