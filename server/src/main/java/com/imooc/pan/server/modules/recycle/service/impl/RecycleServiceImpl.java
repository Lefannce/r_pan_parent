package com.imooc.pan.server.modules.recycle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.imooc.pan.server.common.event.file.FilePhysicalDeleteEvent;
import com.imooc.pan.server.common.event.file.FileRestoreEvent;
import com.imooc.pan.server.modules.file.context.QueryFileListContext;
import com.imooc.pan.server.modules.file.entity.RPanUserFile;
import com.imooc.pan.server.modules.file.enums.DelFlagEnum;
import com.imooc.pan.server.modules.file.service.IUserFileService;
import com.imooc.pan.server.modules.file.vo.RPanUserFileVO;
import com.imooc.pan.server.modules.recycle.context.DeleteContext;
import com.imooc.pan.server.modules.recycle.context.QueryRecycleFileListContext;
import com.imooc.pan.server.modules.recycle.context.RestoreContext;
import com.imooc.pan.server.modules.recycle.service.IRecycleService;
import org.imooc.pan.core.constants.RPanConstants;
import org.imooc.pan.core.exception.RPanBusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 回收站模块业务类
 */
@Service
public class RecycleServiceImpl implements IRecycleService, ApplicationContextAware {
    @Autowired
    private IUserFileService iUserFileService;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 查询用户回收站文件列表
     *
     * @param context
     * @return
     */
    @Override
    public List<RPanUserFileVO> recycles(QueryRecycleFileListContext context) {
        QueryFileListContext queryFileListContext = new QueryFileListContext();
        queryFileListContext.setUserId(context.getUserId());
        queryFileListContext.setDelFlag(DelFlagEnum.YES.getCode());
        return iUserFileService.getFileList(queryFileListContext);
    }


    /**
     * 批量恢复回收站
     * 1,检查操作权限
     * 2,检查是不是可以还原
     * 父文件夹下面是否已经有新建的同名文件,(提示用户文件名已经被占用)
     * <p>
     * 3,执行文件还原操作
     * 4,执行后置函数
     *
     * @param context
     */
    @Override
    public void restore(RestoreContext context) {
        checkRestorePermission(context);
        checkRestoreFilename(context);
        doStore(context);
        afterRestore(context);
    }

    /**
     * 文件彻底删除
     * <p>
     * 1,校验文件权限
     * <p>
     * 2,递归查找所有子文件
     *
     * 3,执行文件删除动作
     *
     * 4,删除后的后置动作
     *
     * @param context
     */
    @Override
    public void delete(DeleteContext context) {
        checkFileDeletePermission(context);
        findAllFileRecords(context);
        doDelete(context);
        afterDelete(context);

    }


/********************************************private*************************************************/


    /**
     * 发布后置操作
     * 1,发布文件还原事件
     *
     * @param context
     */
    private void afterRestore(RestoreContext context) {
        FileRestoreEvent event = new FileRestoreEvent(this, context.getFileIdList());
        applicationContext.publishEvent(event);
    }

    /**
     * 执行文件还原动作
     *
     * @param context
     */
    private void doStore(RestoreContext context) {
        List<RPanUserFile> records = context.getRecords();

        records.stream().forEach(record -> {
            record.setDelFlag(DelFlagEnum.NO.getCode());
            record.setUpdateUser(context.getUserId());
            record.setUpdateTime(new Date());
        });
        boolean updateFlag = iUserFileService.updateBatchById(records);
        if (!updateFlag) throw new RPanBusinessException("修改失败");
    }

    /**
     * 检查需要还原的文件名称是否被占用
     * <p>
     * 1,要还原的文件列表中有同一个文件夹下面相同名称的文件不允许还原
     * <p>
     * 2,要还原的文件当前的父文件夹下面有同名文件不允许还原
     *
     * @param context
     */
    private void checkRestoreFilename(RestoreContext context) {
        List<RPanUserFile> records = context.getRecords();
        //拼接处文件名+分隔符+父id,如果跟上下文数量不同就代表有重复
        //回收站内有删除两次的同名文件
        Set<String> filenameSet = records.stream().map(record -> record.getFilename() + RPanConstants.COMMON_SEPARATOR + record.getParentId()).collect(Collectors.toSet());
        if (filenameSet.size() != records.size()) throw new RPanBusinessException("文件还原失败,该文件中存在同名文件");

        //循环查询数据库中当前父文件夹下是否有未删除的同名文件
        for (RPanUserFile record : records) {
            QueryWrapper queryWrapper = Wrappers.query();
            queryWrapper.eq("user_id", context.getUserId());
            queryWrapper.eq("parent_id", record.getParentId());
            queryWrapper.eq("filename", record.getFilename());
            queryWrapper.eq("del_flag", DelFlagEnum.NO.getCode());

            if (iUserFileService.count(queryWrapper) > 0) {
                throw new RPanBusinessException("文件" + record.getFilename() + "h还原失败,原文件夹中有同名文件");
            }


        }

    }

    /**
     * 检查是否有权限还原
     *
     * @param context
     */
    private void checkRestorePermission(RestoreContext context) {
        List<Long> fileIdList = context.getFileIdList();

        List<RPanUserFile> records = iUserFileService.listByIds(fileIdList);
        if (CollectionUtils.isEmpty(records)) {
            throw new RPanBusinessException("文件还原失败");
        }
        //为用户去重
        Set<Long> userIdSet = records.stream().map(RPanUserFile::getUserId).collect(Collectors.toSet());
        //以上文件不是同一个用户,不能还原
        if (userIdSet.size() > 1) throw new RPanBusinessException("您无权执行文件还原");

        //你传过来的id和要删除的id不配套也不能删除
        if (!userIdSet.contains(context.getUserId())) throw new RPanBusinessException("您无权执行文件还原");
        context.setRecords(records);
    }

     /**
     * 彻底删除后置操作
     *
     * 1,发送一个文件彻底删除事件
     * @param context
     */
    private void afterDelete(DeleteContext context) {
        FilePhysicalDeleteEvent event = new FilePhysicalDeleteEvent(this, context.getAllRecords());
        applicationContext.publishEvent(event);

    }

  /**
   * 执行文件删除的动作
   *
   * @param context
   */
  private void doDelete(DeleteContext context) {
      List<RPanUserFile> allRecords = context.getAllRecords();
      List<Long> fileIdList = allRecords.stream().map(RPanUserFile::getFileId).collect(Collectors.toList());
      if (!iUserFileService.removeByIds(fileIdList)) {
          throw new RPanBusinessException("文件删除失败");
      }
  }
    /**
     * 递归查询所有子文件列表
     * @param context
     */
    private void findAllFileRecords(DeleteContext context) {
        List<RPanUserFile> records = context.getRecords();
        List<RPanUserFile> allRecords = iUserFileService.findAllFileRecords(records);
        context.setAllRecords(allRecords);

    }

    /**
     * 校验文件删除权限
     * @param context
     */
    private void checkFileDeletePermission(DeleteContext context) {
        QueryWrapper queryWrapper = Wrappers.query();
        queryWrapper.eq("user_id",context.getUserId());
        queryWrapper.in("file_id",context.getFileIdList());
        //根据id和fileid查询出数据库中的信息
        List<RPanUserFile> records = iUserFileService.list(queryWrapper);
        //如果为空 或者 大小与context不同就代表有其他用户的数据
        if (CollectionUtils.isEmpty(records) && records.size() != context.getFileIdList().size())
            throw new RPanBusinessException("无权限删除该文件");
        context.setRecords(records);
    }


}
