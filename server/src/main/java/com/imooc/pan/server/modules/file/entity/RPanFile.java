package com.imooc.pan.server.modules.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 物理文件信息表
 * @TableName r_pan_file
 */
@TableName(value ="r_pan_file")
@Data
public class RPanFile implements Serializable {
    /**
     * 文件id
     */
    @TableId(value = "file_id")
    private Long file_id;

    /**
     * 文件名称
     */
    @TableField(value = "filename")
    private String filename;

    /**
     * 文件物理路径
     */
    @TableField(value = "real_path")
    private String real_path;

    /**
     * 文件实际大小
     */
    @TableField(value = "file_size")
    private String file_size;

    /**
     * 文件大小展示字符
     */
    @TableField(value = "file_size_desc")
    private String file_size_desc;

    /**
     * 文件后缀
     */
    @TableField(value = "file_suffix")
    private String file_suffix;

    /**
     * 文件预览的响应头Content-Type的值
     */
    @TableField(value = "file_preview_content_type")
    private String file_preview_content_type;

    /**
     * 文件唯一标识
     */
    @TableField(value = "identifier")
    private String identifier;

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
        RPanFile other = (RPanFile) that;
        return (this.getFile_id() == null ? other.getFile_id() == null : this.getFile_id().equals(other.getFile_id()))
            && (this.getFilename() == null ? other.getFilename() == null : this.getFilename().equals(other.getFilename()))
            && (this.getReal_path() == null ? other.getReal_path() == null : this.getReal_path().equals(other.getReal_path()))
            && (this.getFile_size() == null ? other.getFile_size() == null : this.getFile_size().equals(other.getFile_size()))
            && (this.getFile_size_desc() == null ? other.getFile_size_desc() == null : this.getFile_size_desc().equals(other.getFile_size_desc()))
            && (this.getFile_suffix() == null ? other.getFile_suffix() == null : this.getFile_suffix().equals(other.getFile_suffix()))
            && (this.getFile_preview_content_type() == null ? other.getFile_preview_content_type() == null : this.getFile_preview_content_type().equals(other.getFile_preview_content_type()))
            && (this.getIdentifier() == null ? other.getIdentifier() == null : this.getIdentifier().equals(other.getIdentifier()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getFile_id() == null) ? 0 : getFile_id().hashCode());
        result = prime * result + ((getFilename() == null) ? 0 : getFilename().hashCode());
        result = prime * result + ((getReal_path() == null) ? 0 : getReal_path().hashCode());
        result = prime * result + ((getFile_size() == null) ? 0 : getFile_size().hashCode());
        result = prime * result + ((getFile_size_desc() == null) ? 0 : getFile_size_desc().hashCode());
        result = prime * result + ((getFile_suffix() == null) ? 0 : getFile_suffix().hashCode());
        result = prime * result + ((getFile_preview_content_type() == null) ? 0 : getFile_preview_content_type().hashCode());
        result = prime * result + ((getIdentifier() == null) ? 0 : getIdentifier().hashCode());
        result = prime * result + ((getCreate_user() == null) ? 0 : getCreate_user().hashCode());
        result = prime * result + ((getCreate_time() == null) ? 0 : getCreate_time().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", file_id=").append(file_id);
        sb.append(", filename=").append(filename);
        sb.append(", real_path=").append(real_path);
        sb.append(", file_size=").append(file_size);
        sb.append(", file_size_desc=").append(file_size_desc);
        sb.append(", file_suffix=").append(file_suffix);
        sb.append(", file_preview_content_type=").append(file_preview_content_type);
        sb.append(", identifier=").append(identifier);
        sb.append(", create_user=").append(create_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}