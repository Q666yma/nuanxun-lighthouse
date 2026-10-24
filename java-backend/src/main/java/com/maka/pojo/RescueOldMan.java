package com.maka.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tb_applyform
 */
@Data
public class RescueOldMan implements Serializable {
    private Integer id;

    /**
     * 家属姓名
     */
    private String myName;

    /**
     * 联系方式
     */
    private String tel;

    /**
     * 老人姓名
     */
    private String oldName;

    /**
     * 老人性别
     */
    private String oldGender;

    /**
     * 老人年龄
     */
    private Integer oldAge;

    /**
     * 走失时间
     */
    private Date lostTime;

    /**
     * 智力情况 用数字表示 对应智力表
     */
    private String iq;

    /**
     * 身体素质
     */
    private String health;

    /**
     * 衣着特征
     */
    private String clothing;

    /**
     * 最后一次出现地描述 
     */
    private String lostPlace;

    /**
     * 保存走失人员照片url
     */
    private String oldmanImg;

    /**
     * 0未开始
     * 1进行中
     * 2超过48或者72小时
     */
    private Integer status;

    /**
     * 家属open_id
     */
    private String openId;

    private String oldDesc;

    private static final long serialVersionUID = 1L;
}