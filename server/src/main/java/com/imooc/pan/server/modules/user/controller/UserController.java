package com.imooc.pan.server.modules.user.controller;


import com.imooc.pan.server.common.annotation.LoginIgnore;
import com.imooc.pan.server.common.utils.UserIdUtil;
import com.imooc.pan.server.modules.user.context.*;
import com.imooc.pan.server.modules.user.converter.UserConverter;
import com.imooc.pan.server.modules.user.po.*;
import com.imooc.pan.server.modules.user.service.IUserService;
import com.imooc.pan.server.modules.user.vo.UserInfoVO;
import org.imooc.pan.core.response.R;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模块控制实体
 */
@RestController
@RequestMapping("user")
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private UserConverter userConverter;

    /**
     * 用户注册接口
     *
     * @param userRegisterPO
     * @return
     */
    @LoginIgnore
    @PostMapping("register")
    public R register(@Validated @RequestBody UserRegisterPO userRegisterPO) {
        UserRegisterContext userRegisterContext = userConverter.userRegisterPO2UserRegisterContext(userRegisterPO);
        Long userId = iUserService.register(userRegisterContext);
        return R.data(IdUtil.encrypt((userId)));
    }

    /**
     * 用户登录接口
     *
     * @param userLoginPO
     * @return
     */
    @LoginIgnore
    @PostMapping("login")
    public R login(@Validated @RequestBody UserLoginPO userLoginPO) {
        UserLoginContext userLoginContext = userConverter.userLoginPO2UserLoginContext(userLoginPO);
        String accessToken = iUserService.login(userLoginContext);
        return R.data(accessToken);
    }

    /**
     * 用户登出接口
     *
     * @return
     */
    @PostMapping("exit")
    public R exit() {
        iUserService.exit(UserIdUtil.get());
        return R.success();
    }

    /**
     * 找回密码:用户名校验
     *
     * @param checkUsernamePO
     * @return
     */
    @LoginIgnore
    @PostMapping("username/check")
    public R checkUsername(@Validated @RequestBody CheckUsernamePO checkUsernamePO) {
        CheckUsernameContext checkUsernameContext = userConverter.checkUsernamePO2CheckUsernameContext(checkUsernamePO);
        String question = iUserService.checkUsername(checkUsernameContext);
        return R.data(question);
    }


    /**
     * 校验密保答案
     * @param checkAnswerPO
     * @return
     */
    @LoginIgnore
    @PostMapping("answer/check")
    public  R checkAnswer(@Validated @RequestBody CheckAnswerPO checkAnswerPO){
        CheckAnswerContext checkAnswerContext = userConverter.checkAnswerPO2CheckAnswerContext(checkAnswerPO);
        String token = iUserService.checkAnswer(checkAnswerContext);
        return R.data(token);

    }

    /**
     * 重置密码,需检查token
     * @param resetPasswordPO
     * @return
     */
    @PostMapping("password/reset")
    @LoginIgnore
    public R resetPassword(@Validated @RequestBody ResetPasswordPO resetPasswordPO){
        ResetPasswordContext resetPasswordContext = userConverter.resetPasswordPO2ResetPasswordContext(resetPasswordPO);
        iUserService.resetPassword(resetPasswordContext);
        return R.success();
    }

    /**
     * 在线修改密码
     * @param changePasswordPO
     * @return
     */
     @PostMapping("password/change")
    public R changePassword(@Validated @RequestBody ChangePasswordPO changePasswordPO) {
        ChangePasswordContext changePasswordContext = userConverter.changePasswordPO2ChangePasswordContext(changePasswordPO);
        changePasswordContext.setUserId(UserIdUtil.get());
        iUserService.changePassword(changePasswordContext);
        return R.success();
    }

    /**
     * 查询当前用户基本信息
     * @return
     */
     @GetMapping("/")
    public R<UserInfoVO> info() {
        UserInfoVO userInfoVO = iUserService.info(UserIdUtil.get());
        return R.data(userInfoVO);
    }

}
