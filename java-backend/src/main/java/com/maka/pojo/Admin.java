package com.maka.pojo;

import java.io.Serializable;
import lombok.Data;

/**
 * table_admin
 */
@Data
public class Admin implements Serializable {
    private Integer adminId;

    private String adminName;

    private String adminPassword;

    private String adminPhone;

    private static final long serialVersionUID = 1L;
}