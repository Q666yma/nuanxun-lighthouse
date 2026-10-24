package com.maka.vo;

import lombok.Data;

import java.sql.Time;
import java.util.Date;

@Data
public class RescuingUser {

    private Integer userId;

    private String userName;


    private String area;
    private Date confirmTime;


    public TimeLine transferToTimeLine() {
        TimeLine timeLine = new TimeLine();
        timeLine.setTime(this.confirmTime);

        timeLine.setDesc(area+"附近的"+userName+"加入救援行动");

        return timeLine;
    }


}
