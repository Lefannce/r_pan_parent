package com.imooc.pan.server.modules.file.service;

import com.imooc.pan.server.modules.file.context.FileChunkSaveContext;
import com.imooc.pan.server.modules.file.entity.RPanFileChunk;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Hu Jing
* @description 针对表【r_pan_file_chunk(文件分片信息表)】的数据库操作Service
* @createDate 2023-07-21 22:39:16
*/
public interface IFileChunkService extends IService<RPanFileChunk> {

    /**
     * 文件分片业务实现
     * @param fileChunkSaveContext
     */
    void saveChunkFile(FileChunkSaveContext fileChunkSaveContext);
}
