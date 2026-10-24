package com.maka.service.impl;

import com.maka.mapper.MessageMapper;
import com.maka.service.MessageService;
import com.maka.vo.TimeLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private MessageMapper messageMapper;


    @Autowired
    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public List<TimeLine> getOldManMessage(int oldManId) {
       List<TimeLine> timeLines =   messageMapper.getOldManMessage(oldManId);
       return timeLines;
    }
}
