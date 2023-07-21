package com.imooc.pan.server.modules.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 文件分片信息表
 * @TableName r_pan_file_chunk
 */
@TableName(value ="r_pan_file_chunk")
@Data
public class RPanFileChunk implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文件唯一标识
     */
    @TableField(value = "identifier")
    private String identifier;

    /**
     * 分片真实的存储路径
     */
    @TableField(value = "real_path")
    private String real_path;

    /**
     * 分片编号
     */
    @TableField(value = "chunk_number")
    private Integer chunk_number;

    /**
     * 过期时间
     */
    @TableField(value = "expiration_time")
    private Date expiration_time;

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
        RPanFileChunk other = (RPanFileChunk) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getIdentifier() == null ? other.getIdentifier() == null : this.getIdentifier().equals(other.getIdentifier()))
            && (this.getReal_path() == null ? other.getReal_path() == null : this.getReal_path().equals(other.getReal_path()))
            && (this.getChunk_number() == null ? other.getChunk_number() == null : this.getChunk_number().equals(other.getChunk_number()))
            && (this.getExpiration_time() == null ? other.getExpiration_time() == null : this.getExpiration_time().equals(other.getExpiration_time()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getIdentifier() == null) ? 0 : getIdentifier().hashCode());
        result = prime * result + ((getReal_path() == null) ? 0 : getReal_path().hashCode());
        result = prime * result + ((getChunk_number() == null) ? 0 : getChunk_number().hashCode());
        result = prime * result + ((getExpiration_time() == null) ? 0 : getExpiration_time().hashCode());
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
        sb.append(", id=").append(id);
        sb.append(", identifier=").append(identifier);
        sb.append(", real_path=").append(real_path);
        sb.append(", chunk_number=").append(chunk_number);
        sb.append(", expiration_time=").append(expiration_time);
        sb.append(", create_user=").append(create_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}