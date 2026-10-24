package com.maka.pojo;

import java.io.Serializable;

import lombok.Data;

@Data
public class SkillTag implements Serializable {
    private Integer id;
    private String tagName;
    private String tagCategory;
    private String description;
}