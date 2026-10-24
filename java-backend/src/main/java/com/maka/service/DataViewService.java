package com.maka.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.maka.mapper.DataViewMapper;

public interface DataViewService {
    int getRescuedCount();
    int getRescuingCount();
    int getTotalPeopleNum();
    String getTagJson() throws JsonProcessingException;
}
