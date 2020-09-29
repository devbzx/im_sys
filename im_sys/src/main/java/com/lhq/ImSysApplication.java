package com.lhq;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@SpringBootApplication
@MapperScan(value = "com.lhq.mapper")
@ComponentScan({"com.idworker","com.lhq"})
@EnableCaching
public class ImSysApplication extends SpringBootServletInitializer {
    @Bean
    public SpringUtil getSpringUtil(){
        return new SpringUtil();
    }
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ImSysApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ImSysApplication.class, args);
    }

}
