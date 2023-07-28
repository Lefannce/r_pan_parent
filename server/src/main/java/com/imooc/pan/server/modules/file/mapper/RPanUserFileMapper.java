package com.imooc.pan.server.modules.file.mapper;

import com.imooc.pan.server.modules.file.context.QueryFileListContext;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Hu Jing
* @description 针对表【r_pan_user_file(用户文件信息表)】的数据库操作Mapper
* @createDate 2023-07-21 22:39:16
* @Entity com.imooc.pan.server.modules.file.entity.RPanUserFile
*/
public interface RPanUserFileMapper extends BaseMapper<RPanUserFile> {

    /**
     * 查询user文件列表
     * @param context
     * @return
     */
    List<RPanUserFileVO> selectFileList(@Param("param") QueryFileListContext context);
}




