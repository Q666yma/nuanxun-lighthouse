package com.maka.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maka.mapper.DataViewMapper;
import com.maka.mapper.RescuerMapper;
import com.maka.pojo.Rescuer;
import com.maka.service.DataViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataViewServiceImpl implements DataViewService {

    @Autowired
    DataViewMapper dataViewMapper;
    @Autowired
    RescuerMapper rescuerMapper;
    @Override
    public int getRescuedCount()
    {
        return dataViewMapper.selectRescuedTask().size();
    }
    @Override
    public int getRescuingCount()
    {
        return dataViewMapper.selectRescuingTask().size();
    }
    @Override
    public int getTotalPeopleNum()
    {
        return dataViewMapper.selectAllRescuer().size();
    }

    @Override
    public String getTagJson() throws JsonProcessingException {
        List<Rescuer> rescuers = rescuerMapper.getAvailableRescuers();

        List<List<String>> tagList = new ArrayList<>();
        for (var rescuer : rescuers) {
            if (rescuer.getSkillTags() != null) tagList.add(rescuer.getSkillTags());
        }
        ObjectMapper mapper = new ObjectMapper();

        // 用来记录哪些“元素（序列化后）”已经添加过
        Set<String> allTags = new LinkedHashSet<>();
        for (List<String> jsonArrayStr : tagList) {
            // 把这一段的所有元素都放进 Set 中去重
            allTags.addAll(jsonArrayStr);
        }

        // 把 Set 转回 List（保持顺序），你也可以直接用 Set，但一般用 List 更方便序列化
        List<String> mergedList = new ArrayList<>(allTags);

        // 最终把合并去重后的 List 序列化成一个 JSON 数组字符串
        String mergedJson = mapper.writeValueAsString(mergedList);
        return mergedJson;
    }
}
