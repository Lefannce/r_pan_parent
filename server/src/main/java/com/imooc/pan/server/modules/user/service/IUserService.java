package com.imooc.pan.server.modules.user.service;

import com.imooc.pan.server.modules.user.context.*;
import com.imooc.pan.server.modules.user.entity.RPanUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.imooc.pan.server.modules.user.vo.UserInfoVO;

/**
* @author Hu Jing
* @description 针对表【r_pan_user(用户信息表)】的数据库操作Service
* @createDate 2023-07-21 22:35:18
*/
public interface IUserService extends IService<RPanUser> {

    Long register(UserRegisterContext userRegisterContext);

    String login(UserLoginContext userLoginContext);

    void exit(Long userId);

    String checkUsername(CheckUsernameContext checkUsernameContext);

    String checkAnswer(CheckAnswerContext checkAnswerContext);

    void resetPassword(ResetPasswordContext resetPasswordContext);

    void changePassword(ChangePasswordContext changePasswordContext);

    UserInfoVO info(Long userId);
}
