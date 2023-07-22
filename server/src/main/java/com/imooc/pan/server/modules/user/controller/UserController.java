package com.imooc.pan.server.modules.user.controller;


import com.imooc.pan.server.modules.user.po.UserRegisterPO;
import com.imooc.pan.server.modules.user.service.IUserService;
import org.imooc.pan.core.response.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 该类是用户模块控制实体
 */
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private IUserService iUserService;

    @PostMapping("register")
    public R register(@Validated @RequestBody UserRegisterPO userRegisterPO){
        return null;
    }
}
