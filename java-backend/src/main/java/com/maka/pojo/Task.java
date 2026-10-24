package com.maka.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tasks               — 对应任务表
 * 失踪老人线索任务实体
 */
@Data
public class Task implements Serializable {

    /** 主键 ID */
    private Integer id;

    /** 老人的姓名 */
    private String elderName;

    /** 走失时间 */
    private Date lostTime;

    /** 老人（或线索）照片 URL */
    private String photoUrl;

    /** 语音线索 URL */
    private String audioUrl;

    /** 走失地点 / 线索地点 */
    private String location;

    /** 任务状态：waiting / rescuing / finished 等（枚举存字符串） */
    private String status;

    /** 额外说明信息（JSON / 文本均可） */
    private String extraInfo;

    private static final long serialVersionUID = 1L;
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}
