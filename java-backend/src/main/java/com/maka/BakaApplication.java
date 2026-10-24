package com.maka;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;


@SpringBootApplication

@MapperScan("com.maka.mapper")
public class BakaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BakaApplication.class, args);
    }

}
