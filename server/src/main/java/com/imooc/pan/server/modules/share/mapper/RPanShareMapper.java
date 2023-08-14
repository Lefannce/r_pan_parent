package com.imooc.pan.server.modules.share.mapper;

import com.imooc.pan.server.modules.share.entity.RPanShare;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imooc.pan.server.modules.share.vo.RPanShareUrlListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Hu Jing
* @description 针对表【r_pan_share(用户分享表)】的数据库操作Mapper
* @createDate 2023-07-21 22:43:16
* @Entity com.imooc.pan.server.modules.share.entity.RPanShare
*/
public interface RPanShareMapper extends BaseMapper<RPanShare> {

    /**
     * 查询用户分享列表
     *
     * @param userId
     * @return
     */
    List<RPanShareUrlListVO> selectShareVoListByUserId(@Param("userId") Long userId);

    /**
     * 滚动查询已存在的分享ID集合
     *
     * @param startId
     * @param limit
     * @return
     */
    List<Long> rollingQueryShareId(@Param("startId") long startId, @Param("limit") long limit);
}




