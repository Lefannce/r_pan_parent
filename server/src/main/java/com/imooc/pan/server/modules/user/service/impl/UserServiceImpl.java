package com.imooc.pan.server.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.cache.core.constants.CacheConstants;
import com.imooc.pan.server.modules.file.constants.FileConstants;
import com.imooc.pan.server.modules.file.context.CreateFolderContext;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.user.constants.UserConstants;
import com.imooc.pan.server.modules.user.context.*;
import com.imooc.pan.server.modules.user.converter.UserConverter;
import com.imooc.pan.server.modules.user.entity.RPanUser;
import com.imooc.pan.server.modules.user.service.IUserService;
import com.imooc.pan.server.modules.user.mapper.RPanUserMapper;
import com.imooc.pan.server.modules.user.vo.UserInfoVO;
import io.jsonwebtoken.Jwt;
import org.apache.commons.lang3.StringUtils;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.imooc.pan.core.response.ResponseCode;
import org.imooc.pan.core.utils.IdUtil;
import org.imooc.pan.core.utils.JwtUtil;
import org.imooc.pan.core.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * @author Hu Jing
 * @description 针对表【r_pan_user(用户信息表)】的数据库操作Service实现
 * @createDate 2023-07-21 22:35:18
 */
@Service
public class UserServiceImpl extends ServiceImpl<RPanUserMapper, RPanUser> implements IUserService {

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 用户注册的业务实现
     * 需要实现的功能点：
     * 1、注册用户信息
     * 2、创建新用户的根本目录信息
     * <p>
     * 需要实现的技术难点：
     * 1、该业务是幂等的
     * 2、要保证用户名全局唯一
     * <p>
     * 实现技术难点的处理方案：
     * 1、幂等性通过数据库表对于用户名字段添加唯一索引，我们上有业务捕获对应的冲突异常，转化返回
     *
     * @param userRegisterContext
     * @return
     */
    @Override
    public Long register(UserRegisterContext userRegisterContext) {
        assemblezUserEntity(userRegisterContext);
        doRegister(userRegisterContext);
        createUserRootFolder(userRegisterContext);
        return userRegisterContext.getEntity().getUserId();
    }

    /**
     * 用户登录业务实现
     * 需要实现的功能：
     * 1、用户的登录信息校验
     * 2、生成一个具有时效性的accessToken
     * 3、将accessToken缓存起来，去实现单机登录(只能一台机器登录,另一台登录就剔除前面的设备
     *
     * @param userLoginContext
     * @return
     */
    @Override
    public String login(UserLoginContext userLoginContext) {
        checkLoginInfo(userLoginContext);
        generateAndSaveAccessToken(userLoginContext);
        return userLoginContext.getAccessToken();
    }

    /**
     * 退出登录
     * 1,清除用户登录缓存
     *
     * @param userId
     */
    @Override
    public void exit(Long userId) {
        try {
            Cache cache = cacheManager.getCache(CacheConstants.R_PAN_CACHE_NAME);
            cache.evict(UserConstants.USER_LOGIN_PREFIX + userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RPanBusinessException("用户退出登录失败");
        }
    }

    /**
     * 检查用户名
     *
     * @param checkUsernameContext
     * @return 返回密保问题
     */
    @Override
    public String checkUsername(CheckUsernameContext checkUsernameContext) {
        String question = baseMapper.selectQuestionByUsername(checkUsernameContext.getUsername());
        if (StringUtils.isBlank(question)) {
            throw new RPanBusinessException("没有次用户");
        }
        return question;
    }

    /**
     * 校验密码答案
     *
     * @param checkAnswerContext
     * @return
     */
    @Override
    public String checkAnswer(CheckAnswerContext checkAnswerContext) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("username", checkAnswerContext.getUsername());
        queryWrapper.eq("question", checkAnswerContext.getQuestion());
        queryWrapper.eq("answer", checkAnswerContext.getAnswer());
        int count = count(queryWrapper);
        if (count == 0) {
            throw new RPanBusinessException("密保答案错误");
        }
        return generateAndCheckToken(checkAnswerContext);
    }

    /**
     * 重置用户密码
     * 1,校验重置密码token是否有效
     * 2,重置密码
     *
     * @param resetPasswordContext
     */
    @Override
    public void resetPassword(ResetPasswordContext resetPasswordContext) {
        checkForgetPasswordToken(resetPasswordContext);
        checkAndResetUserPassword(resetPasswordContext);
    }

    /**
     *
     * 在线修改密码
     * 1,校验旧密码
     * 2,重置新密码
     * 3退出当前登录状态
     *
     * @param changePasswordContext
     */
    @Override
    public void changePassword(ChangePasswordContext changePasswordContext) {
        checkOldPassword(changePasswordContext);
        doChangePassword(changePasswordContext);
        exitLoginStatus(changePasswordContext);
    }

    /**
     * 查询用户基本信息实体
     * 查询用户根文件夹实体
     * 拼装vo对象返回
     * @param userId
     * @return
     */
    @Override
    public UserInfoVO info(Long userId) {
        RPanUser entity = getById(userId);
        if (Objects.isNull(entity))
            throw  new RPanBusinessException("用户信息查询失败");

        RPanUserFile rPanUserFile = getUserRootFileInfo(userId);
        if (Objects.isNull(rPanUserFile))
            throw new RPanBusinessException("查询用户根文件夹信息失败");
        //返回拼接好的Vo对象
        return userConverter.assembleUserInfoVO(entity,rPanUserFile);
    }




    /************************************************private************************************************/


    /**
     * 创建用户的根目录信息
     * 为用户创建文件夹,用户上传的所有文件都在此文件夹下
     * 需要协同file模块创建对应实体类,方法实现
     *
     * @param userRegisterContext
     */
    private void createUserRootFolder(UserRegisterContext userRegisterContext) {
        //创建用户文件实体类
        CreateFolderContext createFolderContext = new CreateFolderContext();
        //set父文件id,用户id,父文件文件名
        createFolderContext.setParentId(FileConstants.TOP_PARENT_ID);
        createFolderContext.setUserId(userRegisterContext.getEntity().getUserId());
        createFolderContext.setFolderName(FileConstants.ALL_FILE_CN_STR);
        iUserFileService.createFolder(createFolderContext);
    }

    /**
     * 实现注册用户的业务
     * 需要捕获数据库的唯一索引冲突异常，来实现全局用户名称唯一
     * 1,先获取entity判断是否为空,为空则返回error
     * 2,然后如果捕获到DuplicateKeyException重复异常则返回用户名已存在
     * 3,执行MP的save方法如果保存失败则抛出异常用户注册失败
     *
     * @param userRegisterContext
     */
    private void doRegister(UserRegisterContext userRegisterContext) {
        RPanUser entity = userRegisterContext.getEntity();
        if (Objects.nonNull(entity)) {
            try {
                if (!save(entity)) {
                    throw new RPanBusinessException("用户注册失败");
                }

            }//捕获重复异常
            catch (DuplicateKeyException duplicateKeyException) {
                throw new RPanBusinessException("用户名已存在");
            }
            return;
        }
        throw new RPanBusinessException(ResponseCode.ERROR);

    }

    /**
     * 实体转化,上下文信息转化为用户实体封装进上下文
     *
     * @param userRegisterContext
     */
    private void assemblezUserEntity(UserRegisterContext userRegisterContext) {
        RPanUser entity = userConverter.userRegisterContext2RPanUser(userRegisterContext);
        //stlt 随机生成字符串作为key
        String salt = PasswordUtil.getSalt();
        //生成存入数据库的加密后的密码
        String dbPassword = PasswordUtil.encryptPassword(salt, userRegisterContext.getPassword());
        entity.setUserId(IdUtil.get());
        entity.setSalt(salt);
        entity.setPassword(dbPassword);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        userRegisterContext.setEntity(entity);

    }

    /**
     * 校验用户名密码
     *
     * @param userLoginContext
     */
    private void checkLoginInfo(UserLoginContext userLoginContext) {
        String username = userLoginContext.getUsername();
        String password = userLoginContext.getPassword();

        RPanUser entity = getRPanUserByUsername(username);
        if (Objects.isNull(entity)) {
            throw new RPanBusinessException("用户名不存在");
        }
        String salt = entity.getSalt();
        //使用PasswordUtil给密码加密做对比
        String encryptPassword = PasswordUtil.encryptPassword(salt, password);
        String dbPassword = entity.getPassword();
        if (!Objects.equals(encryptPassword, dbPassword)) {
            throw new RPanBusinessException("密码错误");
        }
        //把entity放入context供下游代码使用
        userLoginContext.setEntity(entity);

    }

    /**
     * 通过用户名获取用户信息
     *
     * @param username
     * @return 返回RPanUser实体类
     */
    private RPanUser getRPanUserByUsername(String username) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("username", username);
        return getOne((queryWrapper));

    }

    /**
     * 生成并保存登录后的token
     *
     * @param userLoginContext
     */
    private void generateAndSaveAccessToken(UserLoginContext userLoginContext) {
        RPanUser entity = userLoginContext.getEntity();
        String accessToken = JwtUtil.generateToken(entity.getUsername(), UserConstants.LOGIN_USER_ID, entity.getUserId(), UserConstants.ONE_DAY_LONG);
        Cache cache = cacheManager.getCache(CacheConstants.R_PAN_CACHE_NAME);
        cache.put(UserConstants.USER_LOGIN_PREFIX + entity.getUserId(), accessToken);
        userLoginContext.setAccessToken(accessToken);
    }

    /**
     * 生成用户忘记密码-校验答案临时token
     * token时效为5分钟
     *
     * @param checkAnswerContext
     * @return
     */
    private String generateAndCheckToken(CheckAnswerContext checkAnswerContext) {
        String token = JwtUtil.generateToken(checkAnswerContext.getUsername(), UserConstants.FORGET_USERNAME, checkAnswerContext.getUsername(), UserConstants.FIVE_MINUTES_LONG);
        return token;


    }

    /**
     * 验证忘记密码token是否有效
     *
     * @param resetPasswordContext
     */
    private void checkForgetPasswordToken(ResetPasswordContext resetPasswordContext) {
        String token = resetPasswordContext.getToken();
        Object o = JwtUtil.analyzeToken(token, UserConstants.FORGET_USERNAME);
        if (Objects.isNull(o)) {
            throw new RPanBusinessException(ResponseCode.TOKEN_EXPIRE);
        }
        //把解析出来的token转换为String
        String tokenUsername = String.valueOf(o);
        if (!Objects.equals(tokenUsername, resetPasswordContext.getUsername())) {
            throw new RPanBusinessException("token错误");

        }
    }

    /**
     * 重置用户密码
     *
     * @param resetPasswordContext
     */
    private void checkAndResetUserPassword(ResetPasswordContext resetPasswordContext) {
        String username = resetPasswordContext.getUsername();
        String password = resetPasswordContext.getPassword();
        RPanUser entity = getRPanUserByUsername(username);
        if (Objects.isNull(entity)) {
            throw new RPanBusinessException("用户信息不存在");
        }
        String newDbPassword = PasswordUtil.encryptPassword(entity.getSalt(), password);
        entity.setPassword(newDbPassword);
        entity.setUpdateTime(new Date());
        if (!updateById(entity)) {
            throw new RPanBusinessException("密码更新失败");
        }

    }

    /**
     * 退出用户登录状态,调用登出方法
     *
     * @param changePasswordContext
     */
    private void exitLoginStatus(ChangePasswordContext changePasswordContext) {
        exit(changePasswordContext.getUserId());
    }

    /**
     * 修改密码信息
     *
     * @param changePasswordContext
     */
    private void doChangePassword(ChangePasswordContext changePasswordContext) {
        String newPassword = changePasswordContext.getNewPassword();
        RPanUser entity = changePasswordContext.getEntity();
        String salt = entity.getSalt();
        String s = PasswordUtil.encryptPassword(salt, newPassword);
        entity.setPassword(s);
        if (!updateById(entity))
            throw new RPanBusinessException("修改密码失败");


    }

    /**
     * 校验旧密码
     * 查询并封装用户实体信息到上下文信息中
     *
     * @param changePasswordContext
     */
    private void checkOldPassword(ChangePasswordContext changePasswordContext) {
        Long userId = changePasswordContext.getUserId();
        String oldPassword = changePasswordContext.getOldPassword();

        RPanUser entity = getById(userId);
        if (Objects.isNull(entity))
            throw new RPanBusinessException("用户信息不存在");
        //把dp的内容set到context
        changePasswordContext.setEntity(entity);
        //传来的老密码
        String encOldPassword = PasswordUtil.encryptPassword(entity.getSalt(), oldPassword);
        //db里的密码
        String dbOldPassword = entity.getPassword();
        if (!Objects.equals(encOldPassword, dbOldPassword))
            throw new RPanBusinessException("旧密码错误");
    }

     /**
     * 获取用户根文件夹信息实体
     * 委托给fileServer
     * @param userId
     * @return
     */
    private RPanUserFile getUserRootFileInfo(Long userId) {
        return iUserFileService.getUserRootFile(userId);

    }
}





