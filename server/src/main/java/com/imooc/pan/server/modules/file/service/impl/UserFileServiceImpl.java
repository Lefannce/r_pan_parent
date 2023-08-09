package com.imooc.pan.server.modules.file.service.impl;




import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.imooc.pan.server.common.event.file.DeleteFileEvent;
import com.imooc.pan.server.common.event.search.UserSearchEvent;
import com.imooc.pan.server.common.utils.HttpUtil;
import com.imooc.pan.server.modules.file.constants.FileConstants;
import com.imooc.pan.server.modules.file.context.*;
import com.imooc.pan.server.modules.file.converter.FileConverter;
import com.imooc.pan.server.modules.file.entity.RPanFile;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.imooc.pan.server.modules.file.enums.DelFlagEnum;
import com.imooc.pan.server.modules.file.enums.FileTypeEnum;
import com.imooc.pan.server.modules.file.enums.FolderFlagEnum;
import com.imooc.pan.server.modules.file.mapper.RPanUserFileMapper;
import com.imooc.pan.server.modules.file.service.IFileChunkService;
import com.imooc.pan.server.modules.file.service.IFileService;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.file.vo.*;
import com.imooc.pan.storage.engine.core.StorageEngine;
import com.imooc.pan.storage.engine.core.context.ReadFileContext;
import org.apache.commons.lang3.StringUtils;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.imooc.pan.core.exception.RPanFrameworkException;
import org.imooc.pan.core.utils.FileUtils;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;
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

    @Autowired
    private StorageEngine storageEngine;

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
        return saveUserFile(createFolderContext.getParentId(), createFolderContext.getFolderName(), FolderFlagEnum.YES, null, null, createFolderContext.getUserId(), null);
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
     * 文件秒传功能
     * <p>
     * 1、判断用户之前是否上传过该文件
     * 2、如果上传过该文件，只需要生成一个该文件和当前用户在指定文件夹下面的关联关系即可
     *
     * @param context
     * @return true 代表用户之前上传过相同文件并成功挂在了关联关系 false 用户没有上传过该文件，请手动执行上传逻辑
     */
    @Override
    public boolean secUpload(SecUploadFileContext context) {
        List<RPanFile> fileList = getFileListByUserIdAndIdentifier(context.getUserId(), context.getIdentifier());
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(fileList)) {
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
        saveUserFile(context.getParentId(), context.getFilename(), FolderFlagEnum.NO, FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())), context.getRecord().getFileId(), context.getUserId(), context.getRecord().getFileSizeDesc());
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
     * <p>
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
        queryWrapper.eq("identifier", context.getIdentifier());
        queryWrapper.eq("create_user", context.getUserId());
        //gt 大于当前时间
        queryWrapper.gt("expiration_time", new Date());

        //查询一个字段,value是上放指定的查询字段,转换成Integer类型
        List<Integer> uploadChunks = iFileChunkService.listObjs(queryWrapper, value -> (Integer) value);

        UploadedChunksVO vo = new UploadedChunksVO();
        vo.setUploadedChunks(uploadChunks);
        return vo;
    }

    /**
     * 合并分片
     * <p>
     * 1,文件分片物理合并
     * 2,保存文件实体记录 委托给fileserver做
     * 3,保存文件关系映射
     *
     * @param context
     */
    @Override
    public void mergeFile(FileChunkMergeContext context) {
        mergeFileChunkAndSaveFile(context);
        saveUserFile(context.getParentId(), context.getFilename(), FolderFlagEnum.NO, FileTypeEnum.getFileTypeCode(FileUtils.getFileSuffix(context.getFilename())), context.getRecord().getFileId(), context.getUserId(), context.getRecord().getFileSizeDesc());


    }

    /**
     * 文件下载实现
     * <p>
     * 1,参数校验,校验文件是否存在,校验文是否属于该用户
     * 2,校验该文件是不是一个文件夹(不支持下载文件夹)
     * 3,执行下载动作
     *
     * @param context
     */
    @Override
    public void download(FileDownloadContext context) {
        RPanUserFile record = getById(context.getFileId());
        checkOperatePermission(record, context.getUserId());
        if (checkIsFolder(record)) {
            throw new RPanFrameworkException("文件夹暂不支持下载");
        }
        doDownload(record, context.getResponse());
    }

    /**
     * 文件预览
     * 1、参数校验：校验文件是否存在，文件是否属于该用户
     * 2、校验该文件是不是一个文件夹
     * 3、执行预览的动作
     *
     * @param context
     */
    @Override
    public void preview(FilePreviewContext context) {
        RPanUserFile record = getById(context.getFileId());
        checkOperatePermission(record, context.getUserId());
        if (checkIsFolder(record)) {
            throw new RPanBusinessException("文件夹暂不支持下载");
        }
        doPreview(record, context.getResponse());
    }

    /**
     * 查询用户的文件夹树
     * <p>
     * 1、查询出该用户的所有文件夹列表
     * 2、在内存中拼装文件夹树
     *
     * @param context
     * @return
     */
    @Override
    public List<FolderTreeNodeVO> getFolderTree(QueryFolderTreeContext context) {
        List<RPanUserFile> folderRecords = queryFolderRecords(context.getUserId());
        List<FolderTreeNodeVO> result = assembleFolderTreeNodeVOList(folderRecords);
        return result;

    }

    /**
     * 文件转移
     * <p>
     * 1、权限校验
     * 2、执行工作
     *
     * @param context
     */
    @Override
    public void transfer(TransferFileContext context) {
        checkTransferCondition(context);
        doTransfer(context);
    }

    /**
     * 文件复制
     * <p>
     * 1、条件校验
     * 2、执行动作
     *
     * @param context
     */
    @Override
    public void copy(CopyFileContext context) {
        checkCopyCondition(context);
        doCopy(context);
    }

    /**
     * 文件列表搜索
     * <p>
     * 1、执行文件搜索
     * 2、拼装文件的父文件夹名称
     * 3、执行文件搜索后的后置动作
     *
     * @param context
     * @return
     */
    @Override
    public List<FileSearchResultVO> search(FileSearchContext context) {
        List<FileSearchResultVO> result = doSearch(context);
        fillParentFilename(result);
        afterSearch(context);
        return result;
    }

    /**
     * 获取面包屑列表
     *
     * 1,获取用户文件夹信息
     *
     * 2拼接需要用到的面包屑列表
     * @param context
     * @return
     */
    @Override
    public List<BreadcrumbVO> getBreadcrumbs(QueryBreadcrumbsContext context) {
        List<RPanUserFile> folderRecords = queryFolderRecords(context.getUserId());
        Map<Long, BreadcrumbVO> prepareBreadcrumbVOMap = folderRecords.stream().map(BreadcrumbVO::transfer).collect(Collectors.toMap(BreadcrumbVO::getId, a -> a));

        BreadcrumbVO currentNode;
        Long fileId = context.getFileId();

        List<BreadcrumbVO> result =Lists.newLinkedList();
        //拼装

        //循环查询父节点,然后添加到头部
        do{
            //获取当前节点
            currentNode = prepareBreadcrumbVOMap.get(fileId);
            if (Objects.nonNull(currentNode)){
            result.add(0,currentNode);
            fileId = currentNode.getParentId();
            }


        }while (Objects.nonNull(currentNode));

        return result;
    }

    /**
     * 递归查询所有的文件子文件信息
     * @param records
     * @return
     */
    @Override
    public List<RPanUserFile> findAllFileRecords(List<RPanUserFile> records) {

        List<RPanUserFile> result = Lists.newArrayList(records);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(result)) {
            return result;
        }
        long folderCount = result.stream().filter(record -> Objects.equals(record.getFolderFlag(), FolderFlagEnum.YES.getCode())).count();
        if (folderCount == 0) {
            return result;
        }
        records.stream().forEach(record -> doFindAllChildRecords(result, record));
        return result;
    }

    /**
     * 递归查询所有的子文件列表
     * 忽略是否删除的标识
     *
     * @param result
     * @param record
     */
    private void doFindAllChildRecords(List<RPanUserFile> result, RPanUserFile record) {
        if (Objects.isNull(record)) {
            return;
        }
        if (!checkIsFolder(record)) {
            return;
        }
        List<RPanUserFile> childRecords = findChildRecordsIgnoreDelFlag(record.getFileId());
        if (org.apache.commons.collections.CollectionUtils.isEmpty(childRecords)) {
            return;
        }
        result.addAll(childRecords);
        childRecords.stream()
                .filter(childRecord -> FolderFlagEnum.YES.getCode().equals(childRecord.getFolderFlag()))
                .forEach(childRecord -> doFindAllChildRecords(result, childRecord));
    }

    /**
     * 查询文件夹下面的文件记录，忽略删除标识
     *
     * @param fileId
     * @return
     */
    private List<RPanUserFile> findChildRecordsIgnoreDelFlag(Long fileId) {
        QueryWrapper queryWrapper = Wrappers.query();
        queryWrapper.eq("parent_id", fileId);
        List<RPanUserFile> childRecords = list(queryWrapper);
        return childRecords;
    }


    /**
     * 递归查询所有的子文件信息
     *
     * @param fileIdList
     * @return
     */
    @Override
    public List<RPanUserFile> findAllFileRecordsByFileIdList(List<Long> fileIdList) {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(fileIdList)) {
            return Lists.newArrayList();
        }
        List<RPanUserFile> records = listByIds(fileIdList);
        if (org.apache.commons.collections.CollectionUtils.isEmpty(records)) {
            return Lists.newArrayList();
        }
        return findAllFileRecords(records);
    }

     /**
     * 实体转换
     *
     * @param records
     * @return
     */
    @Override
    public List<RPanUserFileVO> transferVOList(List<RPanUserFile> records) {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(records)) {
            return Lists.newArrayList();
        }
        return records.stream().map(fileConverter::rPanUserFile2RPanUserFileVO).collect(Collectors.toList());
    }


      /**
     * 文件下载 不校验用户是否是否是上传用户
     *
     * @param context
     */
    @Override
    public void downloadWithoutCheckUser(FileDownloadContext context) {
        RPanUserFile record = getById(context.getFileId());
        if (Objects.isNull(record)) {
            throw new RPanBusinessException("当前文件记录不存在");
        }
        if (checkIsFolder(record)) {
            throw new RPanBusinessException("文件夹暂不支持下载");
        }
        doDownload(record, context.getResponse());
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
    private Long saveUserFile(Long parentId,
                              String filename,
                              FolderFlagEnum folderFlagEnum,
                              Integer fileType,
                              Long realFileId,
                              Long userId,
                              String fileSizeDesc) {
        RPanUserFile entity = assembleRPanFUserFile(parentId, userId, filename, folderFlagEnum, fileType, realFileId, fileSizeDesc);
        if (!save((entity))) {
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
        String newFileName = new StringBuilder((newFilenameWithoutSuffix)).append(FileConstants.CN_LEFT_PARENTHESES_STR).append(count).append(FileConstants.CN_RIGHT_PARENTHESES_STR).append(newFilenameSuffix).toString();
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
        if (!updateById(entity)) throw new RPanBusinessException("文件重命名失败");
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
        if (Objects.isNull(entity)) throw new RPanBusinessException("该文件夹id无效");

        //校验权限???
        if (!Objects.equals(entity.getUserId(), context.getUserId()))
            throw new RPanBusinessException("当前用户没有修改该文件权限");

        if (Objects.equals(entity.getFilename(), context.getNewFilename()))
            throw new RPanBusinessException("不能与原用户名相同");

        QueryWrapper queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", entity.getParentId());
        queryWrapper.eq("filename", context.getNewFilename());
        int count = count(queryWrapper);

        if (count > 0) throw new RPanBusinessException("该文件重复");

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

        if (!update(updateWrapper)) throw new RPanBusinessException("文件删除失败");

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
        if (rPanUserFiles.size() != fileIdList.size()) throw new RPanBusinessException("存在不合法记录");

        //把list集合转换为set集合id为key,然后记录大小,再把ids传入集合然后对比大小,判断是处文件是否一致
        Set<Long> fileIdset = rPanUserFiles.stream().map(RPanUserFile::getFileId).collect(Collectors.toSet());
        int oldSize = fileIdset.size();
        fileIdset.addAll(fileIdList);
        int newSize = fileIdset.size();
        if (oldSize != newSize) throw new RPanBusinessException("存在不合法记录");

        /**
         * 用户权限校验
         * 如果不等于1代表以下文件不是同一个用户所属
         */
        Set<Long> userIdSet = rPanUserFiles.stream().map(RPanUserFile::getUserId).collect(Collectors.toSet());
        if (userIdSet.size() != 1) throw new RPanBusinessException("存在不合法记录");

        //查看set的第一个元素是否等于contextId
        Long dbUserId = userIdSet.stream().findFirst().get();
        if (!Objects.equals(dbUserId, context.getUserId()))
            throw new RPanBusinessException("当前登录用户没有删除此文件权限");
    }

    /**
     * 查询用户文件列表根据文件的唯一标识
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
     *
     * @param context
     */
    private void mergeFileChunkAndSaveFile(FileChunkMergeContext context) {
        FileChunkMergeAndSaveContext fileChunkMergeAndSaveContext = fileConverter.fileChunkMergeContext2FileChunkMergeAndSaveContext(context);
        iFileService.mergeFileChunkAndSaveFile(fileChunkMergeAndSaveContext);
        context.setRecord(fileChunkMergeAndSaveContext.getRecord());

    }

    /**
     * 校验用户下载操作权
     * <p>
     * 1,文件记录必须存在
     * 2,文件下载这必须是该用户
     *
     * @param record
     * @param userId
     */
    private void checkOperatePermission(RPanUserFile record, Long userId) {
        if (Objects.isNull(record)) throw new RPanBusinessException("当前文件记录不存在");
        if (!record.getUserId().equals(userId)) throw new RPanBusinessException("小比崽子你没有该文件操作权限");
    }

    /**
     * 校验是否文件夹
     *
     * @param record
     * @return
     */
    private boolean checkIsFolder(RPanUserFile record) {
        if (Objects.isNull(record)) throw new RPanBusinessException("当前文件记录不存在");
        return FolderFlagEnum.YES.getCode().equals(record.getFolderFlag());
    }

    /**
     * 执行下载操作
     * <p>
     * 1,查询文件的真实路径
     * 2,添加跨域的公共响应头
     * 3,拼装下载文件的名称,长度...相应信息
     * 4,委托文件存储引擎读取文件内容到响应的输出流
     *
     * @param record
     * @param response
     */
    private void doDownload(RPanUserFile record, HttpServletResponse response) {
        RPanFile realFileRecord = iFileService.getById(record.getRealFileId());
        if (Objects.isNull(realFileRecord)) {
            throw new RPanBusinessException("当前的文件记录不存在");
        }
        addCommonResponseHeader(response, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        addDownloadAttribute(response, record, realFileRecord);
        realFile2OutputStream(realFileRecord.getRealPath(), response);
    }

    /**
     * 委托文件存储引擎读取文件内容并写入到输出流
     *
     * @param realPath
     * @param response
     */
    private void realFile2OutputStream(String realPath, HttpServletResponse response) {
        try {
            ReadFileContext context = new ReadFileContext();
            context.setRealPath(realPath);
            context.setOutputStream(response.getOutputStream());
            storageEngine.realFile(context);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件下载失败");
        }
    }

    /**
     * 添加下载文件属性信息
     *
     * @param response
     * @param record
     * @param realFileRecord
     */
    private void addDownloadAttribute(HttpServletResponse response, RPanUserFile record, RPanFile realFileRecord) {
        try {
            response.addHeader(FileConstants.CONTENT_TYPE_STR, FileConstants.CONTENT_DISPOSITION_VALUE_PREFIX_STR + new String(record.getFilename().getBytes(FileConstants.GB2312_STR), FileConstants.IOS_8859_1_STR));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RPanBusinessException("文件下载失败");
        }
        response.setContentLengthLong(Long.valueOf(realFileRecord.getFileSize()));
    }

    /**
     * 添加公共文件读取相应头
     *
     * @param response
     * @param contentTypeValue
     */
    private void addCommonResponseHeader(HttpServletResponse response, String contentTypeValue) {
        response.reset();
        HttpUtil.addCorsResponseHeaders(response);
        response.addHeader(FileConstants.CONTENT_TYPE_STR, contentTypeValue);
        response.setContentType(contentTypeValue);

    }

    /**
     * 执行文件预览的动作
     * 1、查询文件的真实存储路径
     * 2、添加跨域的公共响应头
     * 3、委托文件存储引擎去读取文件内容到响应的输出流中
     *
     * @param record
     * @param response
     */
    private void doPreview(RPanUserFile record, HttpServletResponse response) {
        RPanFile realFileRecord = iFileService.getById(record.getRealFileId());
        if (Objects.isNull(realFileRecord)) {
            throw new RPanBusinessException("当前的文件记录不存在");
        }
        addCommonResponseHeader(response, realFileRecord.getFilePreviewContentType());
        realFile2OutputStream(realFileRecord.getRealPath(), response);
    }


    /**
     * 查询用户所有有效的文件夹信息
     *
     * @param userId
     * @return
     */
    private List<RPanUserFile> queryFolderRecords(Long userId) {
        QueryWrapper queryWrapper = Wrappers.query();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("folder_flag", FolderFlagEnum.YES.getCode());
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        return list(queryWrapper);
    }

    /**
     * 拼装文件夹树列表
     * 难点
     *
     * @param folderRecords
     * @return
     */
    private List<FolderTreeNodeVO> assembleFolderTreeNodeVOList(List<RPanUserFile> folderRecords) {
        if (CollectionUtils.isEmpty(folderRecords)) {
            return Lists.newArrayList();
        }
        //转换context
        List<FolderTreeNodeVO> mappedFolderTreeNodeVOList = folderRecords.stream().map(fileConverter::rPanUserFile2FolderTreeNodeVO).collect(Collectors.toList());
        //分组操作ParentId
        Map<Long, List<FolderTreeNodeVO>> mappedFolderTreeNodeVOMap = mappedFolderTreeNodeVOList.stream().collect(Collectors.groupingBy(FolderTreeNodeVO::getParentId));

        for (FolderTreeNodeVO node : mappedFolderTreeNodeVOList) {
            //获取mappedFolderTreeNodeVOMap中的值
            List<FolderTreeNodeVO> children = mappedFolderTreeNodeVOMap.get(node.getId());
            //获取到就全部放进去
            if (CollectionUtils.isNotEmpty(children)) {
                node.getChildren().addAll(children);
            }
        }
        //直返回顶级的节点,过滤
        return mappedFolderTreeNodeVOList.stream().filter(node -> Objects.equals(node.getParentId(), FileConstants.TOP_PARENT_ID)).collect(Collectors.toList());
    }

    /**
     * 文件转移的条件校验
     * <p>
     * 1、目标文件必须是一个文件夹
     * 2、选中的要转移的文件列表中不能含有目标文件夹以及其子文件夹
     *
     * @param context
     */
    private void checkTransferCondition(TransferFileContext context) {
        Long targetParentId = context.getTargetParentId();
        if (!checkIsFolder(getById(targetParentId))) {
            throw new RPanBusinessException("目标文件不是一个文件夹");
        }
        List<Long> fileIdList = context.getFileIdList();
        List<RPanUserFile> prepareRecords = listByIds(fileIdList);
        context.setPrepareRecords(prepareRecords);
        if (checkIsChildFolder(prepareRecords, targetParentId, context.getUserId())) {
            throw new RPanBusinessException("目标文件夹ID不能是选中文件列表的文件夹ID或其子文件夹ID");
        }
    }

    /**
     * 校验目标文件夹ID是都是要操作的文件记录的文件夹ID以及其子文件夹ID
     * <p>
     * 1、如果要操作的文件列表中没有文件夹，那就直接返回false
     * 2、拼装文件夹ID以及所有子文件夹ID，判断存在即可
     *
     * @param prepareRecords
     * @param targetParentId
     * @param userId
     * @return
     */
    private boolean checkIsChildFolder(List<RPanUserFile> prepareRecords, Long targetParentId, Long userId) {
        prepareRecords = prepareRecords.stream().filter(record -> Objects.equals(record.getFolderFlag(), FolderFlagEnum.YES.getCode())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(prepareRecords)) {
            return false;
        }
        List<RPanUserFile> folderRecords = queryFolderRecords(userId);
        Map<Long, List<RPanUserFile>> folderRecordMap = folderRecords.stream().collect(Collectors.groupingBy(RPanUserFile::getParentId));
        List<RPanUserFile> unavailableFolderRecords = Lists.newArrayList();
        unavailableFolderRecords.addAll(prepareRecords);
        prepareRecords.stream().forEach(record -> findAllChildFolderRecords(unavailableFolderRecords, folderRecordMap, record));
        List<Long> unavailableFolderRecordIds = unavailableFolderRecords.stream().map(RPanUserFile::getFileId).collect(Collectors.toList());
        return unavailableFolderRecordIds.contains(targetParentId);
    }

    /**
     * 查找文件夹的所有子文件夹记录
     *
     * @param unavailableFolderRecords
     * @param folderRecordMap
     * @param record
     */
    private void findAllChildFolderRecords(List<RPanUserFile> unavailableFolderRecords, Map<Long, List<RPanUserFile>> folderRecordMap, RPanUserFile record) {
        if (Objects.isNull(record)) {
            return;
        }
        List<RPanUserFile> childFolderRecords = folderRecordMap.get(record.getFileId());
        if (CollectionUtils.isEmpty(childFolderRecords)) {
            return;
        }
        unavailableFolderRecords.addAll(childFolderRecords);
        childFolderRecords.stream().forEach(childRecord -> findAllChildFolderRecords(unavailableFolderRecords, folderRecordMap, childRecord));
    }


    /**
     * 执行文件转移的动作
     *
     * @param context
     */
    private void doTransfer(TransferFileContext context) {
        List<RPanUserFile> prepareRecords = context.getPrepareRecords();
        prepareRecords.stream().forEach(record -> {
            record.setParentId(context.getTargetParentId());
            record.setUserId(context.getUserId());
            record.setCreateUser(context.getUserId());
            record.setCreateTime(new Date());
            record.setUpdateUser(context.getUserId());
            record.setUpdateTime(new Date());
            handleDuplicateFilename(record);
        });
        if (!updateBatchById(prepareRecords)) {
            throw new RPanBusinessException("文件转移失败");
        }
    }

    /**
     * 文件转移的条件校验
     * <p>
     * 1、目标文件必须是一个文件夹
     * 2、选中的要转移的文件列表中不能含有目标文件夹以及其子文件夹
     *
     * @param context
     */
    private void checkCopyCondition(CopyFileContext context) {
        Long targetParentId = context.getTargetParentId();
        if (!checkIsFolder(getById(targetParentId))) {
            throw new RPanBusinessException("目标文件不是一个文件夹");
        }
        List<Long> fileIdList = context.getFileIdList();
        List<RPanUserFile> prepareRecords = listByIds(fileIdList);
        context.setPrepareRecords(prepareRecords);
        if (checkIsChildFolder(prepareRecords, targetParentId, context.getUserId())) {
            throw new RPanBusinessException("目标文件夹ID不能是选中文件列表的文件夹ID或其子文件夹ID");
        }
    }

    /**
     * 执行文件复制的动作
     *
     * @param context
     */
    private void doCopy(CopyFileContext context) {
        List<RPanUserFile> prepareRecords = context.getPrepareRecords();
        if (CollectionUtils.isNotEmpty(prepareRecords)) {
            List<RPanUserFile> allRecords = Lists.newArrayList();
            prepareRecords.stream().forEach(record -> assembleCopyChildRecord(allRecords, record, context.getTargetParentId(), context.getUserId()));
            if (!saveBatch(allRecords)) {
                throw new RPanBusinessException("文件复制失败");
            }
        }
    }

    /**
     * 拼装当前文件记录以及所有的子文件记录
     *
     * @param allRecords
     * @param record
     * @param targetParentId
     * @param userId
     */
    private void assembleCopyChildRecord(List<RPanUserFile> allRecords, RPanUserFile record, Long targetParentId, Long userId) {
        Long newFileId = IdUtil.get();
        Long oldFileId = record.getFileId();

        record.setParentId(targetParentId);
        record.setFileId(newFileId);
        record.setUserId(userId);
        record.setCreateUser(userId);
        record.setCreateTime(new Date());
        record.setUpdateUser(userId);
        record.setUpdateTime(new Date());
        handleDuplicateFilename(record);

        allRecords.add(record);

        if (checkIsFolder(record)) {
            List<RPanUserFile> childRecords = findChildRecords(oldFileId);
            if (CollectionUtils.isEmpty(childRecords)) {
                return;
            }
            childRecords.stream().forEach(childRecord -> assembleCopyChildRecord(allRecords, childRecord, newFileId, userId));
        }

    }


    /**
     * 查找下一级的文件记录
     *
     * @param parentId
     * @return
     */
    private List<RPanUserFile> findChildRecords(Long parentId) {
        QueryWrapper queryWrapper = Wrappers.query();
        queryWrapper.eq("parent_id", parentId);
        queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());
        return list(queryWrapper);
    }

 /**
     * 搜索文件列表
     *
     * @param context
     * @return
     */
    private List<FileSearchResultVO> doSearch(FileSearchContext context) {
        return baseMapper.searchFile(context);
    }

    /**
     * 填充文件列表的父文件名称
     *
     * @param result
     */
    private void fillParentFilename(List<FileSearchResultVO> result) {
        if (CollectionUtils.isEmpty(result)) {
            return;
        }
        List<Long> parentIdList = result.stream().map(FileSearchResultVO::getParentId).collect(Collectors.toList());
        List<RPanUserFile> parentRecords = listByIds(parentIdList);
        Map<Long, String> fileId2filenameMap = parentRecords.stream().collect(Collectors.toMap(RPanUserFile::getFileId, RPanUserFile::getFilename));
        result.stream().forEach(vo -> vo.setParentFilename(fileId2filenameMap.get(vo.getParentId())));
    }

    /**
     * 搜索的后置操作
     * <p>
     * 1、发布文件搜索的事件
     *
     * @param context
     */
    private void afterSearch(FileSearchContext context) {
        UserSearchEvent event = new UserSearchEvent(this, context.getKeyword(), context.getUserId());
        applicationContext.publishEvent(event);
    }

}




