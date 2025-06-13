package com.juahaki.juahaki.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JuaHakiConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
