package com.imooc.pan.server.modules.file.service;

import com.imooc.pan.server.modules.file.context.FileChunkMergeAndSaveContext;
import com.imooc.pan.server.modules.file.context.FileSaveContext;
import com.imooc.pan.server.modules.file.context.QueryRealFileListContext;
import com.imooc.pan.server.modules.file.entity.RPanFile;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
* @author Hu Jing
* @description 针对表【r_pan_file(物理文件信息表)】的数据库操作Service
* @createDate 2023-07-21 22:39:16
*/
public interface IFileService extends IService<RPanFile> {


    /**
     * 根据条件查询用户的实际文件列表
     *
     * @param context
     * @return
     */
    List<RPanFile> getFileList(QueryRealFileListContext context);



    /**
     * 上传单文件并保存实体记录
     * @param fileSaveContext
     */
    void saveFile(FileSaveContext fileSaveContext);

    void mergeFileChunkAndSaveFile(FileChunkMergeAndSaveContext fileChunkMergeAndSaveContext);
}
