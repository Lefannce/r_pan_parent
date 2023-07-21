package org.imooc.pan.server;

import io.swagger.annotations.Api;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.response.R;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication(scanBasePackages = {RPanConstants.BASE_COMPONENT_SCAN_PATH})
@ServletComponentScan(basePackages = RPanConstants.BASE_COMPONENT_SCAN_PATH)
@RestController
@Api("测试接口")
public class RPanServerLauncher {

    public static void main(String[] args) {
        SpringApplication.run(RPanServerLauncher.class);
    }

    @GetMapping("hello")
    public R hello(@RequestParam(value = "name", required = false) String name) {
        return R.data("hello " + name + "!");
    }

}
