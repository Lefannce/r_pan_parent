package com.imooc.pan.server.modules.user.mapper;

import com.imooc.pan.server.modules.user.entity.RPanUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.lettuce.core.dynamic.annotation.Param;

/**
* @author Hu Jing
* @description 针对表【r_pan_user(用户信息表)】的数据库操作Mapper
* @createDate 2023-07-21 22:35:18
* @Entity com.imooc.pan.server.modules.user.entity.RPanUser
*/
public interface RPanUserMapper extends BaseMapper<RPanUser> {
    /**
     * 通过用户名查询密保问题
     * @param username
     * @return
     */
    String selectQuestionByUsername(@Param("username") String username);
}




