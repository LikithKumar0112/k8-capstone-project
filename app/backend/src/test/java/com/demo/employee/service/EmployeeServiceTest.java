package com.demo.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.employee.exception.DuplicateEmployeeException;
import com.demo.employee.exception.EmployeeNotFoundException;
import com.demo.employee.model.Employee;
import com.demo.employee.repository.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link EmployeeService}, mirroring the business rules verified
 * in the Jenkins capstone but against the new repository-backed implementation.
 * Pure Mockito — no database or Redis required, so it runs in any CI stage.
 */
class EmployeeServiceTest {

    private EmployeeRepository repository;
    private EmployeeService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(EmployeeRepository.class);
        service = new EmployeeService(repository);
    }

    @Test
    void addEmployee_persistsWhenIdIsPositiveAndUnique() {
        Employee alice = new Employee(1, "Alice", "Engineering", 90000);
        when(repository.existsById(1)).thenReturn(false);
        when(repository.save(any(Employee.class))).thenReturn(alice);

        Employee saved = service.addEmployee(alice);

        assertThat(saved).isEqualTo(alice);
        verify(repository).save(alice);
    }

    @Test
    void addEmployee_rejectsNull() {
        assertThatThrownBy(() -> service.addEmployee(null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void addEmployee_rejectsNonPositiveId() {
        Employee bad = new Employee(0, "Bob", "Sales", 50000);
        assertThatThrownBy(() -> service.addEmployee(bad))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void addEmployee_rejectsDuplicateId() {
        Employee dup = new Employee(1, "Carol", "HR", 60000);
        when(repository.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> service.addEmployee(dup))
                .isInstanceOf(DuplicateEmployeeException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findEmployeeById_returnsMatch() {
        Employee alice = new Employee(1, "Alice", "Engineering", 90000);
        when(repository.findById(1)).thenReturn(Optional.of(alice));

        assertThat(service.findEmployeeById(1)).isEqualTo(alice);
    }

    @Test
    void findEmployeeById_throwsWhenMissing() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findEmployeeById(99))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void removeEmployee_deletesWhenPresent() {
        when(repository.existsById(1)).thenReturn(true);

        service.removeEmployee(1);

        verify(repository).deleteById(1);
    }

    @Test
    void removeEmployee_throwsWhenMissing() {
        when(repository.existsById(42)).thenReturn(false);

        assertThatThrownBy(() -> service.removeEmployee(42))
                .isInstanceOf(EmployeeNotFoundException.class);
        verify(repository, never()).deleteById(42);
    }

    @Test
    void getAllEmployees_delegatesToRepository() {
        List<Employee> all = List.of(
                new Employee(1, "Alice", "Engineering", 90000),
                new Employee(2, "Bob", "Sales", 50000));
        when(repository.findAll()).thenReturn(all);

        assertThat(service.getAllEmployees()).hasSize(2).containsExactlyElementsOf(all);
    }
}
