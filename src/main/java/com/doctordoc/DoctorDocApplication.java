package com.doctordoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main Spring Boot application class for Doctor-Doc.
 * Replaces the Struts ActionServlet configuration.
 */
@SpringBootApplication
public class DoctorDocApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(DoctorDocApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DoctorDocApplication.class);
    }
}
