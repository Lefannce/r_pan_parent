package com.imooc.pan.server;

import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.response.R;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {RPanConstants.BASE_COMPONENT_SCAN_PATH})
@ServletComponentScan(basePackages = {RPanConstants.BASE_COMPONENT_SCAN_PATH + ".web"})
@MapperScan(basePackages = {RPanConstants.BASE_COMPONENT_SCAN_PATH + ".server.modules.**.mapper"})
@EnableTransactionManagement
public class RPanServerLauncher {

    public static void main(String[] args) {
        SpringApplication.run(RPanServerLauncher.class);
    }

}