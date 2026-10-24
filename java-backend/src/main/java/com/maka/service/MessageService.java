package com.maka.service;

import com.maka.vo.TimeLine;


import java.util.List;

public interface MessageService {


    List<TimeLine> getOldManMessage(int oldManId);
}
