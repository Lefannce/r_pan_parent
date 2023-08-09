package com.imooc.pan.server.modules.share.service;

import com.imooc.pan.server.modules.share.context.SaveShareFilesContext;
import com.imooc.pan.server.modules.share.entity.RPanShareFile;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Hu Jing
* @description 针对表【r_pan_share_file(用户分享文件表)】的数据库操作Service
* @createDate 2023-07-21 22:43:16
*/
public interface IShareFileService extends IService<RPanShareFile> {

    /**
     * 保存分享和分享文件的关联关系
     * @param saveShareFilesContext
     */
    void saveShareFiles(SaveShareFilesContext saveShareFilesContext);
}
