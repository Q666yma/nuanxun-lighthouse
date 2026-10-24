package com.maka.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TimeLine {

    @JsonFormat( pattern =  "yyyy-MM-dd hh:mm:ss")
    private Date time;


    private String desc;



}
