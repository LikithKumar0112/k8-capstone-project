package com.demo.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the Employee Management backend.
 *
 * <p>This is the multi-tier reincarnation of the in-memory {@code EmployeeService}
 * from the Jenkins capstone: the same {@code Employee} domain and business rules,
 * now backed by MySQL (persistence) and Redis (caching) and exposed over REST.</p>
 */
@SpringBootApplication
@EnableCaching
public class EmployeeManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagementApplication.class, args);
    }
}
