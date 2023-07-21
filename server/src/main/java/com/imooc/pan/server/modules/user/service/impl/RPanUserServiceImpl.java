package com.imooc.pan.server.modules.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.server.modules.user.entity.RPanUser;
import com.imooc.pan.server.modules.user.service.RPanUserService;
import com.imooc.pan.server.modules.user.mapper.RPanUserMapper;
import org.springframework.stereotype.Service;

/**
* @author Hu Jing
* @description 针对表【r_pan_user(用户信息表)】的数据库操作Service实现
* @createDate 2023-07-21 22:35:18
*/
@Service
public class RPanUserServiceImpl extends ServiceImpl<RPanUserMapper, RPanUser>
    implements RPanUserService{

}




