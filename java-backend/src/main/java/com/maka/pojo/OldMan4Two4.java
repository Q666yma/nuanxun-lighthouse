package com.maka.pojo;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 */
@Data
public class OldMan4Two4 {

    private String oldName;
    private String oldGender;
    private String oldManImg;
    private String lostPlace;
    private String lostTime;
    private String iq;
    private String clothing;
    private String tel;

    private String feature;
    public String getIq() { return iq; }
    public String getClothing() { return clothing; }


}
