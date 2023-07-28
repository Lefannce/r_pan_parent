package com.imooc.pan.server.modules.file.service;

import com.imooc.pan.server.modules.file.context.*;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.baomidou.mybatisplus.extension.service.IService;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;

import java.util.List;

/**
 * @author Hu Jing
 * @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Service
 * @createDate 2023-07-21 22:39:16
 */
public interface IUserFileService extends IService<RPanUserFile> {

    /**
     * 创建文件夹信息
     *
     * @param createFolderContext
     * @return
     */
    Long createFolder(CreateFolderContext createFolderContext);

    RPanUserFile getUserRootFile(Long userId);

    /**
     * 查询用户文件列表
     * @param context
     * @return
     */
    List<RPanUserFileVO> getFileList(QueryFileListContext context);

    /**
     * 更新文件名
     */
    void updateFilename(UpdateFilenameContext updateFilenameContext);

    /**
     * 批量删除用户文件
     * @param context
     */
    void deleteFile(DeleteFileContext context);

    /**
     * 文件秒传
     * @param context
     * @return
     */
    boolean secUpload(SecUploadFileContext context);
}
