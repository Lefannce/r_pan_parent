package com.imooc.pan.server.modules.file.controller;

import com.google.common.base.Splitter;
import com.imooc.pan.server.common.utils.UserIdUtil;
import com.imooc.pan.server.modules.file.constants.FileConstants;
import com.imooc.pan.server.modules.file.context.*;
import com.imooc.pan.server.modules.file.converter.FileConverter;
import com.imooc.pan.server.modules.file.enums.DelFlagEnum;
import com.imooc.pan.server.modules.file.po.CreateFolderPO;
import com.imooc.pan.server.modules.file.po.DeleteFilePO;
import com.imooc.pan.server.modules.file.po.SecUploadFilePO;
import com.imooc.pan.server.modules.file.po.UpdateFilenamePO;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import io.swagger.models.auth.In;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.response.R;
import org.imooc.pan.core.utils.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Validated
public class FileController {
    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private FileConverter fileConverter;

    /**
     * 查询文件列表
     *
     * @param parentId  哪个文件
     * @param fileTypes 类型 (有默认值,非必传
     * @return
     */
    @GetMapping("files")
    public R<List<RPanUserFileVO>> list(@NotBlank(message = "父文件夹ID不能为空")
                                        @RequestParam(value = "parentId", required = false) String parentId,
                                        @RequestParam(value = "fileTypes", required = false, defaultValue = FileConstants.ALL_FILE_TYPE) String fileTypes) {
        Long realParentId = -1L;
        List<Integer> fileTypeArray = null;

        if (!Objects.equals(FileConstants.ALL_FILE_TYPE, fileTypes)) {
            //把客户端传来的字符串转换成integer的集合
            fileTypeArray = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileTypes).stream()
                    .map(Integer::valueOf).collect(Collectors.toList());
        }
        QueryFileListContext context = new QueryFileListContext();
        context.setParentId(realParentId);
        context.setFileTypeArray(fileTypeArray);
        context.setUserId(UserIdUtil.get());
        context.setDelFlag(DelFlagEnum.NO.getCode());

        List<RPanUserFileVO> result = iUserFileService.getFileList(context);
        return R.data(result);
    }

    /**
     * 创建文件夹
     *
     * @param createFolderPO
     * @return
     */
    @PostMapping("/file/folder")
    public R<String> createFolder(@Validated @RequestBody CreateFolderPO createFolderPO) {
        CreateFolderContext context = fileConverter.createFolderPO2CreateFolderContext(createFolderPO);
        Long folderId = iUserFileService.createFolder(context);
        return R.success(IdUtil.encrypt(folderId));

    }

    @PutMapping("file")
    public R updateFileName(@Validated @RequestBody UpdateFilenamePO updateFilenamePO) {
        UpdateFilenameContext updateFilenameContext = fileConverter.updateFilenamePO2UpdateFilenameContext(updateFilenamePO);
        iUserFileService.updateFilename(updateFilenameContext);
        return R.success();
    }

    /**
     * 批量删除用户文件
     * @param deleteFilePO
     * @return
     */
    @DeleteMapping("file")
    public R deleteFile(@Validated @RequestBody DeleteFilePO deleteFilePO){
        DeleteFileContext context = fileConverter.deleteFilePO2DeleteFileContext(deleteFilePO);

        String fileIds = deleteFilePO.getFileIds();
        List<Long> fileIdlist = Splitter.on(RPanConstants.COMMON_SEPARATOR).splitToList(fileIds).stream().map(IdUtil::decrypt).collect(Collectors.toList());
        context.setFileIdList(fileIdlist);
        iUserFileService.deleteFile(context);
        return R.success();

    }

    /**
     * 文件秒传
     * @param secUploadFilePO
     * @return
     */
    @PostMapping("file/sec-upload")
    public R secUpload(@Validated @RequestBody SecUploadFilePO secUploadFilePO){
        SecUploadFileContext context = fileConverter.secUploadFilePO2SecUploadFileContext(secUploadFilePO);
        boolean success = iUserFileService.secUpload(context);
        if (success)
            return R.success();

        return R.fail("文件唯一标识不存在,请手动执行文件上传操作");
    }




}
