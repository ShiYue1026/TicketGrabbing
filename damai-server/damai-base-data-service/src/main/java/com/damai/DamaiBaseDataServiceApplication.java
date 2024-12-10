package com.damai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({"com.damai.mapper"})
@SpringBootApplication
public class DamaiBaseDataServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DamaiBaseDataServiceApplication.class, args);
	}

}
