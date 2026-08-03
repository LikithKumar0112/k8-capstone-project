package com.demo.employee.repository;

import com.demo.employee.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Employee} records, backed by MySQL.
 *
 * <p>Replaces the in-memory {@code List<Employee>} of the original Jenkins-capstone
 * service with durable, persistent storage.</p>
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
}
