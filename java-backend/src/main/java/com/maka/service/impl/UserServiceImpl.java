package com.maka.service.impl;

import com.maka.mapper.UserMapper;
import com.maka.pojo.User;
import com.maka.service.UserService;
import com.maka.pojo.Family;
import com.maka.pojo.Rescuer;
import com.maka.dto.UserRegistrationRequest;
import com.maka.dto.UserRegistrationResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

/**
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired private com.maka.support.RoleAccess roleAccess;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean sendVerificationCode(String email) {
        return false;
    }

    @Override
    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        UserRegistrationResponse response = new UserRegistrationResponse();

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword();
        String name = request.getName() == null ? "" : request.getName().trim();
        String role = request.getUserType();
        if (username.length() < 3 || username.length() > 64 || username.matches(".*[\\s\\p{Cntrl}].*")) {
            response.setSuccess(false); response.setMessage("用户名需为 3 至 64 位且不含空白字符"); return response;
        }
        if (password == null || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            response.setSuccess(false); response.setMessage("密码至少 8 位，最多 72 字节"); return response;
        }
        if (!("family".equals(role) || "rescuer".equals(role))) {
            response.setSuccess(false); response.setMessage("请选择家属或救援人员身份"); return response;
        }
        if (name.isEmpty() || name.length() > 100) {
            response.setSuccess(false); response.setMessage("请填写展示姓名，最多 100 字"); return response;
        }
        request.setUsername(username); request.setName(name);
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim().toLowerCase(java.util.Locale.ROOT));

        // 1. 验证用户名是否已存在
        if (userMapper.checkUsernameExists(request.getUsername()) > 0) {
            response.setSuccess(false);
            response.setMessage("用户名已存在");
            return response;
        }

        // 2. 验证邮箱是否已注册
        String email = request.getEmail();
        if (!com.maka.controller.UserController.validEmail(email)) {
            response.setSuccess(false);response.setMessage("请填写有效邮箱");return response;
        }
        if (email != null && !email.isEmpty()) {
            if (userMapper.checkPhoneExists(email) > 0) {
                response.setSuccess(false);
                response.setMessage("该邮箱已注册");
                return response;
            }
        }

        // 3. 创建用户
        String userUuid = UUID.randomUUID().toString();

        User user = new User();
        user.setUuid(userUuid);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(email != null ? email : "");
        user.setGender(request.getGender() != null ? request.getGender() : "M");
        user.setUserType(request.getUserType());

        userMapper.insertUser(user);

        // 4. 根据角色创建相应记录
        if ("rescuer".equals(request.getUserType())) {
            Rescuer rescuer = new Rescuer();
            rescuer.setUuid(userUuid);
            rescuer.setName(request.getName());
            rescuer.setStatus("visitor");
            rescuer.setLocation(request.getLocation());
            rescuer.setTaskIds(new ArrayList<>());
            userMapper.insertRescuer(rescuer);
        } else if ("family".equals(request.getUserType())) {
            Family family = new Family();
            family.setUuid(userUuid);
            family.setName(request.getName());
            family.setTaskIds(new ArrayList<>());
            userMapper.insertFamily(family);
        }

        // 5. 返回成功
        response.setSuccess(true);
        response.setMessage("注册成功");
        response.setUuid(userUuid);

        return response;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isEmpty()) return null;
        username = username.trim();
        if(username.contains("@"))username=username.toLowerCase(java.util.Locale.ROOT);
        User user;

        if (username.contains("@")) {
            user = userMapper.getUserByPhone(username);
        } else {
            user = userMapper.getUserByUsername(username);
        }

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            roleAccess.currentById(user.getUuid());
            return user;
        }

        return null;
    }

    @Override
    public String getUserType(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return "unknown";
        try {
            User user = userMapper.getUserByUuid(uuid);
            if (user == null) return "unknown";
            if (user.getUserType() != null) return user.getUserType();
            if (userMapper.getRescuerByUuid(uuid) != null) return "rescuer";
            if (userMapper.getFamilyByUuid(uuid) != null) return "family";
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public boolean isRescuer(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;
        try {
            User user = userMapper.getUserByUuid(uuid);
            if (user != null && "rescuer".equals(user.getUserType())) return true;
            return userMapper.getRescuerByUuid(uuid) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isFamily(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;
        try {
            User user = userMapper.getUserByUuid(uuid);
            if (user != null && "family".equals(user.getUserType())) return true;
            return userMapper.getFamilyByUuid(uuid) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public User getUserByUUID(String uuid) {
        User user = userMapper.getUserByUuid(uuid);
        return user != null ? user : new User();
    }
}
