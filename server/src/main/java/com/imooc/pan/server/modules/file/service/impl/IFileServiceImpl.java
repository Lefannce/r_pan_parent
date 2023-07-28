package com.imooc.pan.server.modules.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.imooc.pan.server.modules.file.entity.RPanFile;
import com.imooc.pan.server.modules.file.service.IFileService;
import com.imooc.pan.server.modules.file.mapper.RPanFileMapper;
import org.springframework.stereotype.Service;

/**
* @author Hu Jing
* @description 针对表【r_pan_file(物理文件信息表)】的数据库操作Service实现
* @createDate 2023-07-21 22:39:16
*/
@Service
public class IFileServiceImpl extends ServiceImpl<RPanFileMapper, RPanFile>
    implements IFileService {

}




