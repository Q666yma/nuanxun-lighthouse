package com.maka.service;


import com.maka.component.MessageResponse;
import com.maka.pojo.Clue;
import com.maka.pojo.OldMan4Two4;
import com.maka.pojo.RescueOldMan;
import com.maka.query.UserInfo;
import com.maka.vo.OldManInfoView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface RescueOldManService {

    OldMan4Two4 getRandomOldMan();
    List<OldManInfoView> selectRescueOldManByPage(int currentPage, int pageSize);

    Integer getRescueOldManNum();
    List<OldManInfoView> getPageOldManByCondition(int currentPage, int pageSize, String name, String address, String phone);


    MessageResponse updateOldManInfo(OldManInfoView oldManInfoView);

    List<Clue> getClueOfOldMan(int oldMan);
}
