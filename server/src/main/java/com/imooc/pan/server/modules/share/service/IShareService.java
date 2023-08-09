package com.imooc.pan.server.modules.share.service;

import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.share.context.*;
import com.imooc.pan.server.modules.share.entity.RPanShare;
import com.baomidou.mybatisplus.extension.service.IService;
import com.imooc.pan.server.modules.share.vo.RPanShareUrlListVO;
import com.imooc.pan.server.modules.share.vo.RPanShareUrlVO;
import com.imooc.pan.server.modules.share.vo.ShareDetailVO;
import com.imooc.pan.server.modules.share.vo.ShareSimpleDetailVO;

import java.util.List;

/**
* @author Hu Jing
* @description 针对表【r_pan_share(用户分享表)】的数据库操作Service
* @createDate 2023-07-21 22:43:16
*/
public interface IShareService extends IService<RPanShare> {

    /**
     * 创建分享链接业务方法
     * @param context
     * @return
     */
    RPanShareUrlVO create(CreateShareUrlContext context);

    /**
     * 查询分享链接列表
     * @param shareListContext
     * @return
     */
    List<RPanShareUrlListVO> getShares(QueryShareListContext shareListContext);

    /**
     * 取消分享
     * @param context
     */
    void cancelShare(CancelShareContext context);

    /**
     * 校验分享码
     * @param context
     * @return
     */
    String checkShareCode(CheckShareCodeContext context);

    /**
     * 查询分享信息
     * @param context
     * @return
     */
    ShareDetailVO detail(QueryShareDetailContext context);

    /**
     * 查询分享简单详情
     * @param context
     * @return
     */
    ShareSimpleDetailVO simpleDetail(QueryShareSimpleDetailContext context);

    /**
     * 查询下一级文件列表信息
     * @param context
     * @return
     */
    List<RPanUserFileVO> fileList(QueryChildFileListContext context);

    /**
     * 保存至我的网盘
     * @param context
     */
    void saveFiles(ShareSaveContext context);

    /**
     * 分享文件下载
     * @param context
     */
    void download(ShareFileDownloadContext context);

     /**
     * 刷新受影响的对应的分享的状态
     *
     * @param allAvailableFileIdList
     */
    void refreshShareStatus(List<Long> allAvailableFileIdList);
}
