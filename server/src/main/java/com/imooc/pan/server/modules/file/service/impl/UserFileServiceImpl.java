package com.imooc.pan.server.modules.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.server.common.event.file.DeleteFileEvent;
import com.imooc.pan.server.modules.file.constants.FileConstants;
import com.imooc.pan.server.modules.file.context.*;
import com.imooc.pan.server.modules.file.converter.FileConverter;
import com.imooc.pan.server.modules.file.entity.RPanFile;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.imooc.pan.server.modules.file.enums.DelFlagEnum;
import com.imooc.pan.server.modules.file.enums.FileTypeEnum;
import com.imooc.pan.server.modules.file.enums.FolderFlagEnum;
import com.imooc.pan.server.modules.file.service.IFileChunkService;
import com.imooc.pan.server.modules.file.service.IFileService;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.file.mapper.RPanUserFileMapper;
import com.imooc.pan.server.modules.file.vo.FileChunkUploadVO;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.file.vo.UploadedChunksVO;
import org.apache.commons.lang3.StringUtils;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.imooc.pan.core.utils.FileUtils;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Hu Jing
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service实现
 * @createDate 2023-07-21 22:39:16
 */
@Service(value = "userFileServiceImpl")
public class UserFileServiceImpl extends ServiceImpl<RPanUserFileMapper, RPanUserFile> implements IUserFileService, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Autowired
    private IFileService iFileService;

    @Autowired
    private FileConverter fileConverter;

    @Autowired
    private IFileChunkService iFileChunkService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 创建文件夹信息
     * 构建文件夹实体,处理文件命名重复问题
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
     *
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


    /**
     * 查询用户文件列表
     *
     * @param context
     * @return
     */
    @Override
    public List<RPanUserFileVO> getFileList(QueryFileListContext context) {
        return baseMapper.selectFileList(context);
    }

    /**
     * 更新文件名
     * 1,校验更新文件名称的条件(checkUpdateFilenameCondition)
     * 2,执行更新操作
     */
    @Override
    public void updateFilename(UpdateFilenameContext updateFilenameContext) {
        checkUpdateFilenameCondition(updateFilenameContext);
        doUpdateFilename(updateFilenameContext);

    }

    /**
     * 批量删除用户文件
     * 校验删除条件是否符合
     * 执行删除
     * 发布批量删除文件事件给其他模块使用
     *
     * @param context
     */
    @Override
    public void deleteFile(DeleteFileContext context) {
        checkFileDeleteCondition(context);
        dodeleteFile(context);
        afterFileDelete(context);


    }

    /**
     * 文件秒传
     * 1,通过文件唯一标识,查找对应的文件实体记录
     * 2,如果没有查到,直接返回秒传失败
     * 3,如果查询到记录,直接挂在关联关系,返回秒传成功
     *
     * @param context
     * @return
     */
    @Override
    public boolean secUpload(SecUploadFileContext context) {
        List<RPanFile> fileList = getFileListByUserIdAndIdentifier(context.getUserId(), context.getIdentifier());
        if (CollectionUtils.isNotEmpty(fileList)) {
            RPanFile record = fileList.get(RPanConstants.ZERO_INT);
            saveUserFile(context.getParentId(),
                    context.getFilename(),
                    FolderFlagEnum.NO,
                    FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                    record.getFileId(),
                    context.getUserId(),
                    record.getFileSizeDesc());
            return true;
        }
        return false;
    }

    /**
     * 单一文件上传
     * 1,上传文件并保存实体文件记录
     * 2,保存用户文件关系
     *
     * @param context
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void upload(FileUploadContext context) {
        saveFile(context);
        saveUserFile(context.getParentId(),
                context.getFilename(),
                FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                context.getRecord().getFileId(),
                context.getUserId(),
                context.getRecord().getFileSizeDesc());
    }

    /**
     * 文件分片上传
     * 1,上传实体文件
     * 2,保存分片文件记录
     * 3,校验是否全部分片上传完毕
     *
     * @param context
     * @return
     */
    @Override
    public FileChunkUploadVO chunkUpload(FileChunkUploadContext context) {
        FileChunkSaveContext fileChunkSaveContext = fileConverter.fileChunkUploadContext2FileChunkSaveContext(context);
        iFileChunkService.saveChunkFile(fileChunkSaveContext);
        FileChunkUploadVO vo = new FileChunkUploadVO();
        vo.setMergeFlag(fileChunkSaveContext.getMergeFlagEnum().getCode());
        return vo;
    }

    /**
     * 查询用户已上传分片列表
     *
     * 1,查询已上传分片列表
     * 2,封装返回实体
     *
     * @param context
     * @return
     */
    @Override
    public UploadedChunksVO getUploadedChunks(QueryUploadedChunksContext context) {
        QueryWrapper queryWrapper = Wrappers.query();
        //查询指定字段
        queryWrapper.select("chunk_number");
        queryWrapper.eq("identifier",context.getIdentifier());
        queryWrapper.eq("create_user",context.getUserId());
        //gt 大于当前时间
        queryWrapper.gt("expiration_time",new Date());

        //查询一个字段,value是上放指定的查询字段,转换成Integer类型
        List<Integer>  uploadChunks  = iFileChunkService.listObjs(queryWrapper, value -> (Integer) value);

        UploadedChunksVO vo = new UploadedChunksVO();
        vo.setUploadedChunks(uploadChunks);
        return vo;
    }

    /**
     * 合并分片
     *
     * 1,文件分片物理合并
     * 2,保存文件实体记录 委托给fileserver做
     * 3,保存文件关系映射
     * @param context
     */
    @Override
    public void mergeFile(FileChunkMergeContext context) {
       mergeFileChunkAndSaveFile(context);
       saveUserFile(context.getParentId(),
                context.getFilename(),
                FolderFlagEnum.NO,
                FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())),
                context.getRecord().getFileId(),
                context.getUserId(),
                context.getRecord().getFileSizeDesc());


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
        return entity.getFileId();
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
        if (count == 0) {
            return;
        }
        //有同名文件,拼装新的文件名,并set给实体类
        String newFilename = assembleNewFilename(newFilenameWithoutSuffix, count, newFilenameSuffix);
        entity.setFilename(newFilename);
    }

    /**
     * 拼装新文件名称
     * 拼装规则参考操作windows操作系统规范
     *
     * @param newFilenameWithoutSuffix
     * @param count
     * @param newFilenameSuffix
     * @return 新文件夹名称
     */
    private String assembleNewFilename(String newFilenameWithoutSuffix, int count, String newFilenameSuffix) {
        String newFileName = new StringBuilder((newFilenameWithoutSuffix))
                .append(FileConstants.CN_LEFT_PARENTHESES_STR)
                .append(count)
                .append(FileConstants.CN_RIGHT_PARENTHESES_STR)
                .append(newFilenameSuffix)
                .toString();
        return newFileName;
    }

    /**
     * 查找相同文件名数量
     *
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


    /**
     * 执行文件重命名操作
     *
     * @param updateFilenameContext
     */
    private void doUpdateFilename(UpdateFilenameContext updateFilenameContext) {
        RPanUserFile entity = updateFilenameContext.getEntity();
        entity.setFilename(updateFilenameContext.getNewFilename());
        entity.setUpdateUser(updateFilenameContext.getUserId());
        entity.setUpdateTime(new Date());
        if (!updateById(entity))
            throw new RPanBusinessException("文件重命名失败");
    }

    /**
     * 更新文件名称条件校验
     * 文件id有效的
     * 用户有权限更新该文件名称
     * 新旧文件名称不能重复
     * 不能使用当前文件下已有的文件名称
     *
     * @param context
     */

    private void checkUpdateFilenameCondition(UpdateFilenameContext context) {
        Long fileId = context.getFileId();
        RPanUserFile entity = getById(fileId);

        //校验id
        if (Objects.isNull(entity))
            throw new RPanBusinessException("该文件夹id无效");

        //校验权限???
        if (!Objects.equals(entity.getUserId(), context.getUserId()))
            throw new RPanBusinessException("当前用户没有修改该文件权限");

        if (Objects.equals(entity.getFilename(), context.getNewFilename()))
            throw new RPanBusinessException("不能与原用户名相同");

        QueryWrapper queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", entity.getParentId());
        queryWrapper.eq("filename", context.getNewFilename());
        int count = count(queryWrapper);

        if (count > 0)
            throw new RPanBusinessException("该文件重复");

        context.setEntity(entity);
    }

    /**
     * 文件删除的后置操作
     * <p>
     * 1、对外发布文件删除的事件
     *
     * @param context
     */
    private void afterFileDelete(DeleteFileContext context) {
        DeleteFileEvent deleteFileEvent = new DeleteFileEvent(this, context.getFileIdList());
        applicationContext.publishEvent(deleteFileEvent);
    }

    /**
     * 删除文件
     *
     * @param context
     */
    private void dodeleteFile(DeleteFileContext context) {
        List<Long> fileIdList = context.getFileIdList();

        //找到需要删除的文件
        UpdateWrapper updateWrapper = new UpdateWrapper();
        updateWrapper.in("file_id", fileIdList);
        updateWrapper.set("del_flag", DelFlagEnum.YES.getCode());
        updateWrapper.set("update_time", new Date());

        if (!update(updateWrapper))
            throw new RPanBusinessException("文件删除失败");

    }

    /**
     * 删除文件之前的校验
     * 文件id合法性校验
     * 用户权限校验
     *
     * @param context
     */
    private void checkFileDeleteCondition(DeleteFileContext context) {
        List<Long> fileIdList = context.getFileIdList();

        List<RPanUserFile> rPanUserFiles = listByIds(fileIdList);
        //判断查询出来的数量和传入的数量是否相同
        if (rPanUserFiles.size() != fileIdList.size())
            throw new RPanBusinessException("存在不合法记录");

        //把list集合转换为set集合id为key,然后记录大小,再把ids传入集合然后对比大小,判断是处文件是否一致
        Set<Long> fileIdset = rPanUserFiles.stream().map(RPanUserFile::getFileId).collect(Collectors.toSet());
        int oldSize = fileIdset.size();
        fileIdset.addAll(fileIdList);
        int newSize = fileIdset.size();
        if (oldSize != newSize)
            throw new RPanBusinessException("存在不合法记录");

        /**
         * 用户权限校验
         * 如果不等于1代表以下文件不是同一个用户所属
         */
        Set<Long> userIdSet = rPanUserFiles.stream().map(RPanUserFile::getUserId).collect(Collectors.toSet());
        if (userIdSet.size() != 1)
            throw new RPanBusinessException("存在不合法记录");

        //查看set的第一个元素是否等于contextId
        Long dbUserId = userIdSet.stream().findFirst().get();
        if (!Objects.equals(dbUserId, context.getUserId()))
            throw new RPanBusinessException("当前登录用户没有删除此文件权限");
    }

    /**
     * 通过用户id和文件唯一标识查询对应文件是否存在
     *
     * @param userId
     * @param identifier
     * @return
     */
    private List<RPanFile> getFileListByUserIdAndIdentifier(Long userId, String identifier) {
        QueryRealFileListContext context = new QueryRealFileListContext();
        context.setUserId(userId);
        context.setIdentifier(identifier);
        return iFileService.getFileList(context);
    }


//    private RPanFile getFileByUserIdAndIdentifier(Long userId, String identifier) {
//        QueryWrapper queryWrapper = Wrappers.query();
//        queryWrapper.eq("create_user",userId);
//        queryWrapper.eq("identifier",identifier);
//        List<RPanFile> records = iFileService.list(queryWrapper);
//        if (CollectionUtils.isEmpty(records)){
//            return null;
//        }
//        //返回第一条记录作为基准记录
//        return records.get(RPanConstants.ZERO_INT);
//
//    }


    /**
     * 上传文件并保存实体记录
     * 委托给实体文件的Service去完成操作
     *
     * @param context
     */
    private void saveFile(FileUploadContext context) {
        FileSaveContext fileSaveContext = fileConverter.fileUploadContext2FileSaveContext(context);
        iFileService.saveFile(fileSaveContext);
        //record被忽略没有转换过来
        context.setRecord(fileSaveContext.getRecord());

    }

      /**
     * 合并文件分片并保存物理文件记录
     * 委托给ifileService的方法
     * @param context
     */
    private void mergeFileChunkAndSaveFile(FileChunkMergeContext context) {
        FileChunkMergeAndSaveContext fileChunkMergeAndSaveContext = fileConverter.fileChunkMergeContext2FileChunkMergeAndSaveContext(context);
        iFileService.mergeFileChunkAndSaveFile(fileChunkMergeAndSaveContext);
        context.setRecord(fileChunkMergeAndSaveContext.getRecord());

    }

}




