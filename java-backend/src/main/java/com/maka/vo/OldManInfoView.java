package com.maka.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.maka.pojo.RescueOldMan;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 展示给管理员的老人信息
 */
@Data
@Component
public class OldManInfoView {

    private static final Integer FIRST_TIME = 48;
    private static final Integer LAST_TIME = 72;




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
    @JsonFormat(pattern = "yyyy-MM-dd HH:MM:SS")
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
     * 参与救援该老人的人数
     */
    private Integer rescueNum;
    /**
     * 0未开始
     * 1进行中
     * 2超过48或者72小时
     */
    private Integer status;


    private String oldDesc;
    public OldManInfoView (){}
    public OldManInfoView(RescueOldMan rescueOldMan) {
        this.id = rescueOldMan.getId();
        this.myName = rescueOldMan.getMyName();
        this.tel = rescueOldMan.getTel();
        this.oldName = rescueOldMan.getOldName();
        this.oldGender = rescueOldMan.getOldGender();
        this.oldAge = rescueOldMan.getOldAge();
        this.lostTime = rescueOldMan.getLostTime();
        this.iq = rescueOldMan.getIq();
        this.health = rescueOldMan.getHealth();
        this.clothing = rescueOldMan.getClothing();
        this.lostPlace = rescueOldMan.getLostPlace();
        this.status = rescueOldMan.getStatus();
        this.oldDesc = rescueOldMan.getOldDesc();
    }

    public RescueOldMan transferToRescueOldMan(){
        RescueOldMan rescueOldMan = new RescueOldMan();

        rescueOldMan.setClothing(this.clothing);
        rescueOldMan.setHealth(this.health);
        rescueOldMan.setId(this.id);
        rescueOldMan.setIq(this.iq);
        rescueOldMan.setLostPlace(this.lostPlace);
        rescueOldMan.setLostTime(this.lostTime);
        rescueOldMan.setMyName(this.myName);
        rescueOldMan.setOldAge(this.oldAge);
        rescueOldMan.setOldDesc(this.oldDesc);
        rescueOldMan.setOldGender(this.oldGender);
        rescueOldMan.setOldName(this.oldName);
        rescueOldMan.setStatus(this.status);
        rescueOldMan.setTel(this.tel);



        return rescueOldMan;
    }

    public String getStatus() {
        if (status == 0) {
            return "未开始";
        } else if (status == 1) {
            Date current = new Date();
            long time = ((current.getTime() -lostTime.getTime()) / (60 * 60 * 1000));
            if(time>=FIRST_TIME){
                return "超过48小时";
            }else if(time>=LAST_TIME){
                return "超过72小时";
            }
            return "进行中";
        } else {
            return "已结束";
        }
    }


}
