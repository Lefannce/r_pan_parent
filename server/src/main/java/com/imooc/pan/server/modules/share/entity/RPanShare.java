package com.imooc.pan.server.modules.share.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户分享表
 * @TableName r_pan_share
 */
@TableName(value ="r_pan_share")
@Data
public class RPanShare implements Serializable {
    /**
     * 分享id
     */
    @TableId(value = "share_id")
    private Long share_id;

    /**
     * 分享名称
     */
    @TableField(value = "share_name")
    private String share_name;

    /**
     * 分享类型（0 有提取码）
     */
    @TableField(value = "share_type")
    private Integer share_type;

    /**
     * 分享类型（0 永久有效；1 7天有效；2 30天有效）
     */
    @TableField(value = "share_day_type")
    private Integer share_day_type;

    /**
     * 分享有效天数（永久有效为0）
     */
    @TableField(value = "share_day")
    private Integer share_day;

    /**
     * 分享结束时间
     */
    @TableField(value = "share_end_time")
    private Date share_end_time;

    /**
     * 分享链接地址
     */
    @TableField(value = "share_url")
    private String share_url;

    /**
     * 分享提取码
     */
    @TableField(value = "share_code")
    private String share_code;

    /**
     * 分享状态（0 正常；1 有文件被删除）
     */
    @TableField(value = "share_status")
    private Integer share_status;

    /**
     * 分享创建人
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
        RPanShare other = (RPanShare) that;
        return (this.getShare_id() == null ? other.getShare_id() == null : this.getShare_id().equals(other.getShare_id()))
            && (this.getShare_name() == null ? other.getShare_name() == null : this.getShare_name().equals(other.getShare_name()))
            && (this.getShare_type() == null ? other.getShare_type() == null : this.getShare_type().equals(other.getShare_type()))
            && (this.getShare_day_type() == null ? other.getShare_day_type() == null : this.getShare_day_type().equals(other.getShare_day_type()))
            && (this.getShare_day() == null ? other.getShare_day() == null : this.getShare_day().equals(other.getShare_day()))
            && (this.getShare_end_time() == null ? other.getShare_end_time() == null : this.getShare_end_time().equals(other.getShare_end_time()))
            && (this.getShare_url() == null ? other.getShare_url() == null : this.getShare_url().equals(other.getShare_url()))
            && (this.getShare_code() == null ? other.getShare_code() == null : this.getShare_code().equals(other.getShare_code()))
            && (this.getShare_status() == null ? other.getShare_status() == null : this.getShare_status().equals(other.getShare_status()))
            && (this.getCreate_user() == null ? other.getCreate_user() == null : this.getCreate_user().equals(other.getCreate_user()))
            && (this.getCreate_time() == null ? other.getCreate_time() == null : this.getCreate_time().equals(other.getCreate_time()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getShare_id() == null) ? 0 : getShare_id().hashCode());
        result = prime * result + ((getShare_name() == null) ? 0 : getShare_name().hashCode());
        result = prime * result + ((getShare_type() == null) ? 0 : getShare_type().hashCode());
        result = prime * result + ((getShare_day_type() == null) ? 0 : getShare_day_type().hashCode());
        result = prime * result + ((getShare_day() == null) ? 0 : getShare_day().hashCode());
        result = prime * result + ((getShare_end_time() == null) ? 0 : getShare_end_time().hashCode());
        result = prime * result + ((getShare_url() == null) ? 0 : getShare_url().hashCode());
        result = prime * result + ((getShare_code() == null) ? 0 : getShare_code().hashCode());
        result = prime * result + ((getShare_status() == null) ? 0 : getShare_status().hashCode());
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
        sb.append(", share_id=").append(share_id);
        sb.append(", share_name=").append(share_name);
        sb.append(", share_type=").append(share_type);
        sb.append(", share_day_type=").append(share_day_type);
        sb.append(", share_day=").append(share_day);
        sb.append(", share_end_time=").append(share_end_time);
        sb.append(", share_url=").append(share_url);
        sb.append(", share_code=").append(share_code);
        sb.append(", share_status=").append(share_status);
        sb.append(", create_user=").append(create_user);
        sb.append(", create_time=").append(create_time);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}