package com.example.pproject.Config;

import com.example.pproject.Util.DateTimeUtil;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public DateTimeUtil dateTimeUtil() {
        return new DateTimeUtil();
    }
}
