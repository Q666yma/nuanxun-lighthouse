package com.maka.pojo;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.maka.vo.TimeLine;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;

/**
 * 上传的线索
 */
@Component
public class Clue {

    private int clueId;


    /**
     * 老人的编号ID
     */
    private int oldManId;
    /**
     * 上传线索的队员ID
     */
    @NotNull(message = "用户ID不能为空")
    @NotEmpty(message = "用户ID不能为空")
    private int userId;

    @NotEmpty(message = "地点不能为空")
    @NotNull(message = "地点不能为空")
    private String cluePlace;


    /**
     * 时间
     */
    @NotEmpty(message = "时间不能为空")
    @NotNull(message = "时间不能为空")
    private String clueDate;

    /**
     * 是否发现老人
     */

    private int clueFindOldMan;
    /**
     * 老人是否出现过这个地方
     */

    private int clueAppear;



    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;



    private  String desc;


    public Clue() {
    }

    public TimeLine transferToTimeLine(String oldManName){
        TimeLine timeLine = new TimeLine();
        StringBuilder builder =new StringBuilder();
        if(this.getClueFindOldMan()>0){
            builder.append("在").append(this.getCluePlace()).append("找到了老人");
        }else{
            if(this.getClueAppear()>0){
                builder.append(oldManName).append("曾经来过").append(this.getCluePlace());
            }else{
                builder.append(oldManName).append("没有来过").append(this.getCluePlace());
            }
        }
        timeLine.setDesc(builder.toString());
        timeLine.setTime(uploadTime);
        return timeLine;
    }


    public Clue(int clueId, int oldManId, @NotNull(message = "用户ID不能为空") @NotEmpty(message = "用户ID不能为空") int userId, @NotEmpty(message = "地点不能为空") @NotNull(message = "地点不能为空") String cluePlace, String clueImg, @NotEmpty(message = "时间不能为空") @NotNull(message = "时间不能为空") String clueDate, int clueFindOldMan, int clueAppear, Date uploadTime,String desc) {
        this.clueId = clueId;
        this.oldManId = oldManId;
        this.userId = userId;
        this.cluePlace = cluePlace;

        this.clueDate = clueDate;
        this.clueFindOldMan = clueFindOldMan;
        this.clueAppear = clueAppear;
        this.uploadTime = uploadTime;
        this.desc = desc;

    }

    @Override
    public String toString() {
        return "Clue{" +
                "clueId=" + clueId +
                ", oldManId=" + oldManId +
                ", userId=" + userId +
                ", cluePlace='" + cluePlace + '\'' +
                ", clueDate='" + clueDate + '\'' +
                ", clueFindOldMan=" + clueFindOldMan +
                ", clueAppear=" + clueAppear +
                ", uploadTime=" + uploadTime +
                '}';
    }




    public String getDesc() {
        return desc;
    }

    public int getClueId() {
        return clueId;
    }

    public int getOldManId() {
        return oldManId;
    }

    public int getUserId() {
        return userId;
    }

    public String getCluePlace() {
        return cluePlace;
    }



    public String getClueDate() {
        return clueDate;
    }

    public int getClueFindOldMan() {
        return clueFindOldMan;
    }

    public int getClueAppear() {
        return clueAppear;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
}
