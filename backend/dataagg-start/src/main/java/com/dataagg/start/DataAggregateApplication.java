package com.dataagg.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.dataagg")
@MapperScan(basePackages = "com.dataagg.service.mapper")
public class DataAggregateApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAggregateApplication.class, args);
    }
}
