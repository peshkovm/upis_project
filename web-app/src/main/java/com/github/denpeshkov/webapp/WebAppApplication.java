package com.github.denpeshkov.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class WebAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebAppApplication.class, args);
    }
}

@Component
class EnumConverter implements Converter<String, Company> {

    @Override
    public Company convert(String source) {
        return Company.valueOf(source.toUpperCase());
    }
}
