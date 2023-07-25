package com.imooc.pan.server.modules.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.server.modules.file.constants.FileConstants;
import com.imooc.pan.server.modules.file.context.CreateFolderContext;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.imooc.pan.server.modules.file.enums.DelFlagEnum;
import com.imooc.pan.server.modules.file.enums.FolderFlagEnum;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.file.mapper.RPanUserFileMapper;
import org.apache.commons.lang3.StringUtils;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Hu Jing
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service实现
 * @createDate 2023-07-21 22:39:16
 */
@Service(value = "userFileServiceImpl")
public class UserFileServiceImpl extends ServiceImpl<RPanUserFileMapper, RPanUserFile> implements IUserFileService {


    /**
     * 创建文件夹信息
     *
     * @param createFolderContext
     * @return
     */
    @Override
    public Long createFolder(CreateFolderContext createFolderContext) {
        return saveUserFile(createFolderContext.getParentId(),
                createFolderContext.getFolderName(),
                FolderFlagEnum.YES, null,
                null, createFolderContext.getUserId(),
                null);
    }

    /**
     * 查询用户根文件夹信息
     * @param userId
     * @return
     */
    @Override
    public RPanUserFile getUserRootFile(Long userId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("parent_id", FileConstants.TOP_PARENT_ID);
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        queryWrapper.eq("folder_flag", FolderFlagEnum.YES.getCode());
        return getOne(queryWrapper);
    }

    /************************************************private************************************************/

    /**
     * 保存用户文件的映射记录
     * 1,先构建一个用户文件实体把参数参传入
     * 2,然后保存到userfile表中
     *
     * @param parentId
     * @param filename
     * @param folderFlagEnum
     * @param fileType       文件类型（1 普通文件 2 压缩文件 3 excel 4 word 5 pdf 6 txt 7 图片 8 音频 9 视频 10 ppt 11 源码文件 12 csv）
     * @param realFileId
     * @param userId
     * @param fileSizeDesc
     * @return
     */
    private Long saveUserFile(Long parentId, String filename, FolderFlagEnum folderFlagEnum, Integer fileType, Long realFileId, Long userId, String fileSizeDesc) {
        RPanUserFile entity = assembleRPanFUserFile(parentId, userId, filename, folderFlagEnum, fileType, realFileId, fileSizeDesc);
        if (!save(entity)) {
            throw new RPanBusinessException("保存文件信息失败");
        }
        return entity.getUserId();
    }

    /**
     * 拼装userfile文件实体类
     * 1,构建并填充实体信息
     * 2,处理文件命名一致问题
     * a文件夹下面上传文件b,然后又上传一条,新相同文件名使用windows规则(1)
     *
     * @param parentId
     * @param userId
     * @param filename
     * @param folderFlagEnum
     * @param fileType
     * @param realFileId
     * @param fileSizeDesc
     * @return
     */
    private RPanUserFile assembleRPanFUserFile(Long parentId, Long userId, String filename, FolderFlagEnum folderFlagEnum, Integer fileType, Long realFileId, String fileSizeDesc) {
        RPanUserFile entity = new RPanUserFile();
        entity.setFileId(IdUtil.get());
        entity.setUserId(userId);
        entity.setParentId(parentId);
        entity.setRealFileId(realFileId);
        entity.setFilename(filename);
        entity.setFolderFlag(folderFlagEnum.getCode());
        entity.setFileSizeDesc(fileSizeDesc);
        entity.setFileType(fileType);
        entity.setDelFlag(DelFlagEnum.NO.getCode());
        entity.setCreateUser(userId);
        entity.setCreateTime(new Date());
        entity.setUpdateUser(userId);
        entity.setUpdateTime(new Date());

        handleDuplicateFilename(entity);
        return entity;
    }

    /**
     * 处理用户重复名称
     * 如果同一文件夹下面有文件名称重复
     * 按照系统级规则重命名文件
     *
     * @param entity
     */
    private void handleDuplicateFilename(RPanUserFile entity) {
        String filename = entity.getFilename();
        //不带后缀的文件名,文件后缀
        String newFilenameWithoutSuffix;
        String newFilenameSuffix;
        //判断.的位置最后出现的地方
        int newFilenamePointPosition = filename.lastIndexOf(RPanConstants.POINT_STR);
        //newFilenamePointPosition等于-1证明没有找到点,文件名没有后缀
        if (newFilenamePointPosition == RPanConstants.MINUS_ONE_INT) {
            newFilenameWithoutSuffix = filename;
            newFilenameSuffix = StringUtils.EMPTY;
        } else
        //此外证明有后缀,需要手动截取
        {
            //从0截取到newFilenamePointPosition就是.之前的文件名
            newFilenameWithoutSuffix = filename.substring(RPanConstants.ZERO_INT, newFilenamePointPosition);
            //replace()替换,把文件夹名替换成空字符串就找出了后缀
            newFilenameSuffix = filename.replace(newFilenameWithoutSuffix, StringUtils.EMPTY);

        }
        //获取重复文件夹数量
        int count = getDuplicateFilename(entity, newFilenameWithoutSuffix);
        //无同名文件返回
        if (count == 0){
            return;
        }
        //有同名文件,拼装新的文件名,并set给实体类
        String  newFilename = assembleNewFilename(newFilenameWithoutSuffix, count, newFilenameSuffix);
        entity.setFilename(newFilename);
    }

    /**
     * 拼装新文件名称
     * 拼装规则参考操作windows操作系统规范
     * @param newFilenameWithoutSuffix
     * @param count
     * @param newFilenameSuffix
     * @return 新文件夹名称
     */
    private String assembleNewFilename(String newFilenameWithoutSuffix, int count, String newFilenameSuffix) {
    String  newFileName = new StringBuilder((newFilenameWithoutSuffix))
            .append(FileConstants.CN_LEFT_PARENTHESES_STR)
            .append(count)
            .append(FileConstants.CN_RIGHT_PARENTHESES_STR)
            .append(newFilenameSuffix)
            .toString();
    return newFileName;
    }

    /**
     *查找相同文件名数量
     * @param entity
     * @param newFilenameWithoutSuffix
     * @return 相同的数量
     */
    private int getDuplicateFilename(RPanUserFile entity, String newFilenameWithoutSuffix) {
        QueryWrapper queryWrapper = new QueryWrapper();
        //同一文件夹下的文件
        queryWrapper.eq("parent_id", entity.getParentId());
        //文件夹就只查文件夹,不是文件夹就不查文件夹
        queryWrapper.eq("folder_flag", entity.getFolderFlag());
        queryWrapper.eq("user_id", entity.getUserId());
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        queryWrapper.likeLeft("filename", newFilenameWithoutSuffix);
        return count(queryWrapper);
    }
}




