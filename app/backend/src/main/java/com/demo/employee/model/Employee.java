package com.demo.employee.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.util.Objects;

/**
 * Employee domain entity — carried over from the Jenkins capstone
 * ({@code id}, {@code name}, {@code department}, {@code salary}) and mapped to
 * the {@code employees} table in MySQL.
 *
 * <p>Implements {@link Serializable} so instances can be cached in Redis.</p>
 */
@Entity
@Table(name = "employees")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Client-supplied identifier. Business rule: must be positive and unique. */
    @Id
    @Positive(message = "Employee id must be a positive value")
    private int id;

    @NotBlank(message = "Employee name must not be blank")
    private String name;

    @NotBlank(message = "Employee department must not be blank")
    private String department;

    @PositiveOrZero(message = "Employee salary must not be negative")
    private double salary;

    protected Employee() {
        // Required by JPA.
    }

    public Employee(int id, String name, String department, double salary) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Employee employee = (Employee) o;
        return id == employee.id
                && Double.compare(employee.salary, salary) == 0
                && Objects.equals(name, employee.name)
                && Objects.equals(department, employee.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, department, salary);
    }

    @Override
    public String toString() {
        return "Employee{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", department='" + department + '\''
                + ", salary=" + salary
                + '}';
    }
}
