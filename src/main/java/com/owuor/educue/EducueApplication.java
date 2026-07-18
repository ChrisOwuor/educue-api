package com.owuor.educue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EducueApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducueApplication.class, args);
    }

}
