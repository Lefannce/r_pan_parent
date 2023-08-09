package com.imooc.pan.server.modules.share.controller;

import com.google.common.base.Splitter;
import com.imooc.pan.server.common.annotation.LoginIgnore;
import com.imooc.pan.server.common.annotation.NeedShareCode;
import com.imooc.pan.server.common.utils.ShareIdUtil;
import com.imooc.pan.server.common.utils.UserIdUtil;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.share.context.*;
import com.imooc.pan.server.modules.share.converter.ShareConverter;
import com.imooc.pan.server.modules.share.po.CancelSharePO;
import com.imooc.pan.server.modules.share.po.CheckShareCodePO;
import com.imooc.pan.server.modules.share.po.CreateShareUrlPO;
import com.imooc.pan.server.modules.share.po.ShareSavePO;
import com.imooc.pan.server.modules.share.service.IShareService;
import com.imooc.pan.server.modules.share.vo.RPanShareUrlListVO;
import com.imooc.pan.server.modules.share.vo.RPanShareUrlVO;
import com.imooc.pan.server.modules.share.vo.ShareDetailVO;
import com.imooc.pan.server.modules.share.vo.ShareSimpleDetailVO;
import io.swagger.annotations.Api;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.imooc.pan.core.response.R;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "分享模块")
@RestController
@Validated
public class ShareController {

    @Autowired
    private IShareService iShareService;

    @Autowired
    private ShareConverter shareConverter;

    /**
     * 创建分享链接
     *
     * @param createShareUrlPO
     * @return
     */
    @PostMapping("share")
    public R<RPanShareUrlVO> create(@Validated @RequestBody CreateShareUrlPO createShareUrlPO) {
        CreateShareUrlContext context = shareConverter.createShareUrlPO2CreateShareUrlContext(createShareUrlPO);

        String shareFileIds = createShareUrlPO.getShareFileIds();
        List<Long> shareFileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(shareFileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());

        context.setShareFileIdList(shareFileIdList);

        RPanShareUrlVO vo = iShareService.create(context);
        return R.data(vo);

    }


    /**
     * 查询分享链接列表
     *
     * @return
     */
    @GetMapping("shares")
    public R<List<RPanShareUrlListVO>> getShares() {
        QueryShareListContext shareListContext = new QueryShareListContext();
        shareListContext.setUserId(UserIdUtil.get());
        List<RPanShareUrlListVO> result = iShareService.getShares(shareListContext);
        return R.data(result);

    }

    /**
     * 取消分享
     *
     * @param cancelSharePO
     * @return
     */

    @DeleteMapping("shares")
    public R cancelShare(@Validated @RequestBody CancelSharePO cancelSharePO) {
        CancelShareContext context = new CancelShareContext();
        List<Long> cancelIds = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(cancelSharePO.getShareIds()).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        context.setShareIdList(cancelIds);

        iShareService.cancelShare(context);
        return R.success();

    }

    /**
     * 校验分享码
     *
     * @param checkShareCodePO
     * @return
     */
    @LoginIgnore
    @PostMapping("share/code/check")
    public R<String> checkShareCode(@Validated @RequestBody CheckShareCodePO checkShareCodePO) {
        CheckShareCodeContext context = new CheckShareCodeContext();

        context.setShareId(IdUtil.decrypt(checkShareCodePO.getShareId()));
        context.setShareCode(checkShareCodePO.getShareCode());

        String token = iShareService.checkShareCode(context);
        return R.data(token);
    }

    /**
     * 查询分享详情
     * <p>
     * 通过校验分享码接口来的token
     *
     * @return
     */
    @LoginIgnore
    @NeedShareCode
    @GetMapping("share")
    public R<ShareDetailVO> detail() {
        QueryShareDetailContext context = new QueryShareDetailContext();
        context.setShareId(ShareIdUtil.get());
        ShareDetailVO vo = iShareService.detail(context);
        return R.data(vo);
    }

    /**
     * 查询分享简单详情(未填写分享码)
     *
     * @param shareId
     * @return
     */
    @LoginIgnore
    @GetMapping("share/simple")
    public R<ShareSimpleDetailVO> simpleDetail(@NotBlank(message = "分享的ID不能为空") @RequestParam(value = "shareId", required = false) String shareId) {
        QueryShareSimpleDetailContext context = new QueryShareSimpleDetailContext();
        context.setShareId(IdUtil.decrypt(shareId));
        ShareSimpleDetailVO vo = iShareService.simpleDetail(context);
        return R.data(vo);
    }

    /**
     * 获取下一级文件列表
     *
     * @param parentId
     * @return
     */
    @GetMapping("share/file/list")
    @NeedShareCode
    @LoginIgnore
    public R<List<RPanUserFileVO>> fileList(@NotBlank(message = "文件的父ID不能为空") @RequestParam(value = "parentId", required = false) String parentId) {
        QueryChildFileListContext context = new QueryChildFileListContext();
        context.setShareId(ShareIdUtil.get());
        context.setParentId(IdUtil.decrypt(parentId));
        List<RPanUserFileVO> result = iShareService.fileList(context);
        return R.data(result);
    }

    /**
     * 保存至我的网盘
     *
     * @param shareSavePO
     * @return
     */
    @NeedShareCode
    @PostMapping("share/save")
    public R saveFiles(@Validated @RequestBody ShareSavePO shareSavePO) {
        ShareSaveContext context = new ShareSaveContext();

        String fileIds = shareSavePO.getFileIds();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        context.setFileIdList(fileIdList);

        context.setTargetParentId(IdUtil.decrypt(shareSavePO.getTargetParentId()));
        context.setUserId(UserIdUtil.get());
        context.setShareId(ShareIdUtil.get());

        iShareService.saveFiles(context);
        return R.success();
    }

    /**
     * 分享文件下载功能
     *
     * @param fileId
     * @param response
     */
    @GetMapping("share/file/download")
    @NeedShareCode
    public void download(@NotBlank(message = "文件ID不能为空") @RequestParam(value = "fileId", required = false) String fileId,
                         HttpServletResponse response) {
        ShareFileDownloadContext context = new ShareFileDownloadContext();
        context.setFileId(IdUtil.decrypt(fileId));
        context.setShareId(ShareIdUtil.get());
        context.setUserId(UserIdUtil.get());
        context.setResponse(response);
        iShareService.download(context);
    }

}
