package com.damai.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenApi() {

        return new OpenAPI()
                .info(new Info()
                        .title("抢票系统")
                        .version("1.0")
                        .description("基于SpringCloud的高并发抢票系统")
                        .contact(new Contact()
                                .name("ShiYue20011026")
                        ));

    }

}
