package com.imooc.pan.server.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 错误日志表
 * @TableName r_pan_error_log
 */
@TableName(value ="r_pan_error_log")
@Data
public class RPanErrorLog implements Serializable {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日志内容
     */
    @TableField(value = "log_content")
    private String log_content;

    /**
     * 日志状态：0 未处理 1 已处理
     */
    @TableField(value = "log_status")
    private Integer log_status;

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
        RPanErrorLog other = (RPanErrorLog) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getLog_content() == null ? other.getLog_content() == null : this.getLog_content().equals(other.getLog_content()))
            && (this.getLog_status() == null ? other.getLog_status() == null : this.getLog_status().equals(other.getLog_status()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()))
            && (this.getUpdate_user() == null ? other.getUpdate_user() == null : this.getUpdate_user().equals(other.getUpdate_user()))
            && (this.getUpdate_time() == null ? other.getUpdate_time() == null : this.getUpdate_time().equals(other.getUpdate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getLog_content() == null) ? 0 : getLog_content().hashCode());
        result = prime * result + ((getLog_status() == null) ? 0 : getLog_status().hashCode());
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
        sb.append(", id=").append(id);
        sb.append(", log_content=").append(log_content);
        sb.append(", log_status=").append(log_status);
        sb.append(", create_user=").append(create_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", update_user=").append(update_user);
        sb.append(", update_time=").append(update_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}