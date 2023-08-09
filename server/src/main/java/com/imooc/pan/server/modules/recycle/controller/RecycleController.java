package com.imooc.pan.server.modules.recycle.controller;

import com.google.common.base.Splitter;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.recycle.context.DeleteContext;
import com.imooc.pan.server.modules.recycle.context.QueryRecycleFileListContext;
import com.imooc.pan.server.modules.recycle.context.RestoreContext;
import com.imooc.pan.server.modules.recycle.po.DeletePO;
import com.imooc.pan.server.modules.recycle.po.RestorePO;
import com.imooc.pan.server.modules.recycle.service.IRecycleService;
import io.swagger.annotations.Api;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.response.R;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 回收站模块
 */
@RestController
@Api(tags = "回收站模块")
@Validated
@CrossOrigin

public class RecycleController {
    @Autowired
    private IRecycleService iRecycleService;


    /**
     * 获取回收站列表
     * @return
     */
    @GetMapping("recycles")
    public R<List<RPanUserFileVO>> recycles(){
        QueryRecycleFileListContext context = new QueryRecycleFileListContext();
        context.setUserId(IdUtil.get());
        List<RPanUserFileVO>  result = iRecycleService.recycles(context);
        return R.data(result);

    }


    /**
     * 回收站文件批量还原
     * @param restorePO
     * @return
     */
    @PostMapping("recycle/restore")
    public R restore(@Validated @RequestBody RestorePO restorePO){
        RestoreContext context = new RestoreContext();
        context.setUserId(IdUtil.get());

        String fileIds = restorePO.getFileIds();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        context.setFileIdList(fileIdList);

        iRecycleService.restore(context);

        return R.success();

    }

    /**
     * 彻底删除文件
     * @param deletePO
     * @return
     */
    @DeleteMapping("recycle")
    public R delete(@Validated @RequestBody DeletePO deletePO){
        DeleteContext context = new DeleteContext();
        context.setUserId(IdUtil.get());

        String fileIds = deletePO.getFileIds();
        List<Long> fileIdList = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        context.setFileIdList(fileIdList);
        iRecycleService.delete(context);
        return R.success();
    }

}
