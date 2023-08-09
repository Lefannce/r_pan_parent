package com.imooc.pan.server.modules.recycle.service;

import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.recycle.context.DeleteContext;
import com.imooc.pan.server.modules.recycle.context.QueryRecycleFileListContext;
import com.imooc.pan.server.modules.recycle.context.RestoreContext;

import java.util.List;

public interface IRecycleService {
    /**
     * 查询回收站列表
     * @param context
     * @return
     */
    List<RPanUserFileVO> recycles(QueryRecycleFileListContext context);

    /**
     * 批量恢复删回收站
     * @param context
     */
    void restore(RestoreContext context);

    /**
     * 彻底删除文件
     * @param context
     */
    void delete(DeleteContext context);
}
