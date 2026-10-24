package com.maka.controller;

import com.maka.support.RoleAccess;
import com.maka.service.impl.ContentService;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    @Autowired private RoleAccess access;
    @Autowired private ContentService service;
    @GetMapping
    public Map<String,Object> list(HttpSession session,@RequestParam(defaultValue="")String kind,@RequestParam(defaultValue="false")boolean mine,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit){return service.list(access.current(session),kind,"",mine,page,limit,false);}
    @PostMapping
    public Map<String,Object> save(HttpSession session,@RequestBody Map<String,Object> body){return service.save(access.requireRole(session,"family","rescuer"),body,false);}
    @DeleteMapping("/{id}")
    public Map<String,Object> delete(HttpSession session,@PathVariable long id){return service.delete(access.current(session),id,false);}
}
