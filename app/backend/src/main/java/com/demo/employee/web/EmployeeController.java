package com.demo.employee.web;

import com.demo.employee.model.Employee;
import com.demo.employee.service.EmployeeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for employee management.
 *
 * <pre>
 *   GET    /api/employees        list all (Redis-cached)
 *   GET    /api/employees/{id}   find by id (Redis-cached)
 *   POST   /api/employees        add (validates positive + unique id)
 *   DELETE /api/employees/{id}   remove
 * </pre>
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Employee> getAll() {
        return service.getAllEmployees();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable int id) {
        return service.findEmployeeById(id);
    }

    @PostMapping
    public ResponseEntity<Employee> add(@Valid @RequestBody Employee employee) {
        Employee saved = service.addEmployee(employee);
        return ResponseEntity
                .created(URI.create("/api/employees/" + saved.getId()))
                .body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable int id) {
        service.removeEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
