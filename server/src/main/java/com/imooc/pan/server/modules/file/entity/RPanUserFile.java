package com.imooc.pan.server.modules.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户文件信息表
 * @TableName r_pan_user_file
 */
@TableName(value ="r_pan_user_file")
@Data
public class RPanUserFile implements Serializable {
    /**
     * 文件记录ID
     */
    @TableId(value = "file_id")
    private Long file_id;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long user_id;

    /**
     * 上级文件夹ID,顶级文件夹为0
     */
    @TableField(value = "parent_id")
    private Long parent_id;

    /**
     * 真实文件id
     */
    @TableField(value = "real_file_id")
    private Long real_file_id;

    /**
     * 文件名
     */
    @TableField(value = "filename")
    private String filename;

    /**
     * 是否是文件夹 （0 否 1 是）
     */
    @TableField(value = "folder_flag")
    private Integer folder_flag;

    /**
     * 文件大小展示字符
     */
    @TableField(value = "file_size_desc")
    private String file_size_desc;

    /**
     * 文件类型（1 普通文件 2 压缩文件 3 excel 4 word 5 pdf 6 txt 7 图片 8 音频 9 视频 10 ppt 11 源码文件 12 csv）
     */
    @TableField(value = "file_type")
    private Integer file_type;

    /**
     * 删除标识（0 否 1 是）
     */
    @TableField(value = "del_flag")
    private Integer del_flag;

    /**
     * 创建人
     */
    @TableField(value = "create_user")
    private Long create_user;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date create_time;

    /**
     * 更新人
     */
    @TableField(value = "update_user")
    private Long update_user;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date update_time;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        RPanUserFile other = (RPanUserFile) that;
        return (this.getFile_id() == null ? other.getFile_id() == null : this.getFile_id().equals(other.getFile_id()))
            && (this.getUser_id() == null ? other.getUser_id() == null : this.getUser_id().equals(other.getUser_id()))
            && (this.getParent_id() == null ? other.getParent_id() == null : this.getParent_id().equals(other.getParent_id()))
            && (this.getReal_file_id() == null ? other.getReal_file_id() == null : this.getReal_file_id().equals(other.getReal_file_id()))
            && (this.getFilename() == null ? other.getFilename() == null : this.getFilename().equals(other.getFilename()))
            && (this.getFolder_flag() == null ? other.getFolder_flag() == null : this.getFolder_flag().equals(other.getFolder_flag()))
            && (this.getFile_size_desc() == null ? other.getFile_size_desc() == null : this.getFile_size_desc().equals(other.getFile_size_desc()))
            && (this.getFile_type() == null ? other.getFile_type() == null : this.getFile_type().equals(other.getFile_type()))
            && (this.getDel_flag() == null ? other.getDel_flag() == null : this.getDel_flag().equals(other.getDel_flag()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()))
            && (this.getUpdate_user() == null ? other.getUpdate_user() == null : this.getUpdate_user().equals(other.getUpdate_user()))
            && (this.getUpdate_time() == null ? other.getUpdate_time() == null : this.getUpdate_time().equals(other.getUpdate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFile_id() == null) ? 0 : getFile_id().hashCode());
        result = prime * result + ((getUser_id() == null) ? 0 : getUser_id().hashCode());
        result = prime * result + ((getParent_id() == null) ? 0 : getParent_id().hashCode());
        result = prime * result + ((getReal_file_id() == null) ? 0 : getReal_file_id().hashCode());
        result = prime * result + ((getFilename() == null) ? 0 : getFilename().hashCode());
        result = prime * result + ((getFolder_flag() == null) ? 0 : getFolder_flag().hashCode());
        result = prime * result + ((getFile_size_desc() == null) ? 0 : getFile_size_desc().hashCode());
        result = prime * result + ((getFile_type() == null) ? 0 : getFile_type().hashCode());
        result = prime * result + ((getDel_flag() == null) ? 0 : getDel_flag().hashCode());
        result = prime * result + ((getCreate_user() == null) ? 0 : getCreate_user().hashCode());
        result = prime * result + ((getCreate_time() == null) ? 0 : getCreate_time().hashCode());
        result = prime * result + ((getUpdate_user() == null) ? 0 : getUpdate_user().hashCode());
        result = prime * result + ((getUpdate_time() == null) ? 0 : getUpdate_time().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", file_id=").append(file_id);
        sb.append(", user_id=").append(user_id);
        sb.append(", parent_id=").append(parent_id);
        sb.append(", real_file_id=").append(real_file_id);
        sb.append(", filename=").append(filename);
        sb.append(", folder_flag=").append(folder_flag);
        sb.append(", file_size_desc=").append(file_size_desc);
        sb.append(", file_type=").append(file_type);
        sb.append(", del_flag=").append(del_flag);
        sb.append(", create_user=").append(create_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", update_user=").append(update_user);
        sb.append(", update_time=").append(update_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}