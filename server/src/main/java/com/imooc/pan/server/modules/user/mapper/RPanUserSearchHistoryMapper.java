package com.imooc.pan.server.modules.user.mapper;

import com.imooc.pan.server.modules.user.context.QueryUserSearchHistoryContext;
import com.imooc.pan.server.modules.user.entity.RPanUserSearchHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imooc.pan.server.modules.user.vo.UserSearchHistoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Hu Jing
* @description 针对表【r_pan_user_search_history(用户搜索历史表)】的数据库操作Mapper
* @createDate 2023-07-21 22:35:18
* @Entity com.imooc.pan.server.modules.user.entity.RPanUserSearchHistory
*/
public interface RPanUserSearchHistoryMapper extends BaseMapper<RPanUserSearchHistory> {

    List<UserSearchHistoryVO> selectSearchHistories(@Param("param") QueryUserSearchHistoryContext context);
}




