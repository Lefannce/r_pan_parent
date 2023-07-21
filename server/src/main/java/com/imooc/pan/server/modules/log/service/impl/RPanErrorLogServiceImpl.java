package com.imooc.pan.server.modules.log.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.server.modules.log.entity.RPanErrorLog;
import com.imooc.pan.server.modules.log.service.RPanErrorLogService;
import com.imooc.pan.server.modules.log.mapper.RPanErrorLogMapper;
import org.springframework.stereotype.Service;

/**
* @author Hu Jing
* @description 针对表【r_pan_error_log(错误日志表)】的数据库操作Service实现
* @createDate 2023-07-21 22:41:03
*/
@Service
public class RPanErrorLogServiceImpl extends ServiceImpl<RPanErrorLogMapper, RPanErrorLog>
    implements RPanErrorLogService{

}




