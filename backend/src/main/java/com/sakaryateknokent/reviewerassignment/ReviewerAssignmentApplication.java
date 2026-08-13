package com.sakaryateknokent.reviewerassignment;

import com.sakaryateknokent.reviewerassignment.config.ProjectFileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(ProjectFileStorageProperties.class)
public class ReviewerAssignmentApplication {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(ReviewerAssignmentApplication.class, args);
    }

}
