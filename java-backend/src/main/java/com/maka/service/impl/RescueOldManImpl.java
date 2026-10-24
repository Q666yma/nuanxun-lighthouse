package com.maka.service.impl;

import com.maka.component.MessageResponse;
import com.maka.mapper.RescueOldManMapper;
import com.maka.pojo.Clue;
import com.maka.pojo.OldMan4Two4;
import com.maka.pojo.RescueOldMan;
import com.maka.query.UserInfo;
import com.maka.service.RescueOldManService;
import com.maka.vo.OldManInfoView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RescueOldManImpl implements RescueOldManService {

    private RescueOldManMapper rescueOldManMapper;


    @Autowired
    public RescueOldManImpl(RescueOldManMapper rescueOldManMapper) {
        this.rescueOldManMapper = rescueOldManMapper;
    }

    @Override
    public OldMan4Two4 getRandomOldMan() {
        return rescueOldManMapper.getRandomOldMan();
    }

    @Override
    public List<Clue> getClueOfOldMan(int oldMan) {
        return  rescueOldManMapper.getClueOfOldMan(oldMan);
    }

    @Override
    public List<OldManInfoView> selectRescueOldManByPage(int currentPage, int pageSize) {
        List<RescueOldMan>  oldManList=  rescueOldManMapper.selectRescueOldManByPage((currentPage-1)*pageSize,pageSize);
        ArrayList<OldManInfoView> oldManInfoViews = new ArrayList<>(oldManList.size());
        //转化成展示的对象
        for(RescueOldMan rescueOldMan:oldManList){
            OldManInfoView oldManInfoView = transfer(rescueOldMan);
            oldManInfoViews.add(oldManInfoView);
        }
        return oldManInfoViews;
    }


    @Override
    public List<OldManInfoView> getPageOldManByCondition(int currentPage, int pageSize, String name, String lostPlace, String phone) {
        List<RescueOldMan> oldManList = rescueOldManMapper.selectPageOldManByCondition((currentPage - 1) * pageSize, pageSize, name, lostPlace, phone);
        //转化成展示的对象
        ArrayList<OldManInfoView> oldManInfoViews = new ArrayList<>(oldManList.size());
        for(RescueOldMan rescueOldMan:oldManList){
            OldManInfoView oldManInfoView = transfer(rescueOldMan);
            oldManInfoViews.add(oldManInfoView);
        }
        return oldManInfoViews;
    }


    @Override
    public Integer getRescueOldManNum() {
        return rescueOldManMapper.getRescueOldManNum();
    }

    public OldManInfoView transfer(RescueOldMan rescueOldMan){
        OldManInfoView oldManInfoView = new OldManInfoView(rescueOldMan);
        oldManInfoView.setRescueNum(rescueOldManMapper.getOneOldManRescueNum(rescueOldMan.getId()));
        return oldManInfoView;
    }


    @Override
    public MessageResponse updateOldManInfo(OldManInfoView oldManInfoView) {

        int i = rescueOldManMapper.updateByPrimaryKeySelective(oldManInfoView.transferToRescueOldMan());
        if(i>0){
            return MessageResponse.success();
        }
        return MessageResponse.systemError("稍后再试");
    }
}
