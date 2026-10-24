package com.maka.controller;

import com.maka.component.MessageResponse;
import com.maka.query.PageRequest;
import com.maka.query.PageResponse;
import com.maka.query.UserInfo;
import com.maka.service.UserService;
import com.maka.service.MailService;
import com.maka.service.impl.UserServiceImpl;
import com.maka.dto.UserRegistrationRequest;
import com.maka.dto.UserRegistrationResponse;
import com.maka.pojo.User;

import com.mysql.cj.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @Autowired private com.maka.support.RoleAccess roleAccess;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // ===== 原手机验证码接口（保留兼容性）=====
    @PostMapping("/sendVerificationCode")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");

        if (phone == null || phone.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "手机号不能为空");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        boolean sent = userService.sendVerificationCode(phone);

        Map<String, Object> result = new HashMap<>();
        if (sent) {
            result.put("success", true);
            result.put("message", "验证码已发送");
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "验证码发送失败");
            return ResponseEntity.unprocessableEntity().body(result);
        }
    }

    // ===== 新增：邮箱验证码接口 =====
    @PostMapping("/sendEmailCode")
    public ResponseEntity<?> sendEmailCode(@RequestBody Map<String, String> request) {
        String email = normalizedEmail(request.get("email"));
        Map<String, Object> result = new HashMap<>();

        if (email == null || email.isEmpty()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        // 简单邮箱格式校验
        if (!validEmail(email)) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return ResponseEntity.badRequest().body(result);
        }

        // 生成6位数字验证码
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));

        // 存入 Redis，有效期 5 分钟，key = email_code:<email>
        try {
            redisTemplate.opsForValue().set("email_code:" + email, code, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("Redis存储邮箱验证码失败", e);
            result.put("success", false);
            result.put("message", "验证码生成失败，请稍后重试");
            return ResponseEntity.status(500).body(result);
        }

        // 发送邮件
        try {
            String subject = "【暖寻灯塔】注册验证码";
            String body = "您好！\n\n"
                    + "您的注册验证码为：" + code + "\n\n"
                    + "验证码5分钟内有效，请勿泄露给他人。\n\n"
                    + "如非本人操作，请忽略此邮件。\n\n"
                    + "——暖寻灯塔团队";
            mailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            logger.warn("发送邮箱验证码失败");
            redisTemplate.delete("email_code:" + email);
            result.put("success", false);
            result.put("message", "邮件发送失败，请检查邮箱地址是否正确");
            return ResponseEntity.status(500).body(result);
        }

        result.put("success", true);
        result.put("message", "验证码已发送至 " + email + "，请查收");
        return ResponseEntity.ok(result);
    }

    // ===== 新增：校验邮箱验证码（注册时调用）=====
    public static String normalizedEmail(String email) {return email==null?"":email.trim().toLowerCase(java.util.Locale.ROOT);}
    public static boolean validEmail(String email) {return email!=null&&email.length()<=320&&email.matches("[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]+");}
    public boolean verifyEmailCode(String email, String inputCode) {
        if(!validEmail(email)||inputCode==null||!inputCode.matches("[0-9]{6}"))return false;
        // Compare and consume in one Redis operation so concurrent registrations cannot reuse a code.
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> check=new org.springframework.data.redis.core.script.DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",Long.class);
        return Long.valueOf(1).equals(redisTemplate.execute(check,java.util.List.of("email_code:"+email),inputCode));
    }
    private ResponseEntity<UserRegistrationResponse> registrationError(String message) {
        UserRegistrationResponse response=new UserRegistrationResponse();response.setSuccess(false);response.setMessage(message);return ResponseEntity.badRequest().body(response);
    }
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(@RequestBody UserRegistrationRequest request) {
        if(!java.util.Set.of("family","rescuer").contains(String.valueOf(request.getUserType())))return registrationError("请选择家属或搜救人员身份");
        String email=normalizedEmail(request.getEmail());request.setEmail(email);
        if(!validEmail(email))return registrationError("请填写有效邮箱并获取验证码");
        String code=request.getEmailCode()==null?"":request.getEmailCode().trim();
        if(!code.matches("[0-9]{6}"))return registrationError("请输入6位邮箱验证码");
        try {
            if(!verifyEmailCode(email,code))return registrationError("邮箱验证码错误或已过期");
            return ResponseEntity.ok(userService.register(request));
        } catch(org.springframework.dao.DuplicateKeyException ex) {
            return registrationError("用户名或邮箱已被注册");
        } catch(org.springframework.data.redis.RedisConnectionFailureException ex) {
            return registrationError("验证码服务暂不可用，请稍后重试");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpSession session, javax.servlet.http.HttpServletRequest currentRequest) {
        String username = request.get("username");
        String password = request.get("password");
        Boolean remember = Boolean.valueOf(request.get("remember"));

        User user = userService.login(username, password);

        if (user != null) {
            roleAccess.currentById(user.getUuid());
            // Rotate the session identifier after successful authentication.
            session.invalidate();
            session = currentRequest.getSession(true);
            session.setAttribute("userId", user.getUuid());
            session.setAttribute("userType", user.getUserType());
            session.setAttribute("authVersion",roleAccess.currentById(user.getUuid()).get("authVersion"));

            if (remember != null && remember) {
                session.setMaxInactiveInterval(7 * 24 * 60 * 60);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("userType", user.getUserType());

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping({"/getCurrentUser", "/me"})
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        return ResponseEntity.ok(roleAccess.current(session));
    }

    @PostMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> saveProfile(@RequestBody Map<String, String> request, HttpSession session) {
        String uuid = (String) roleAccess.current(session).get("uuid");
        String name = request.getOrDefault("name", "").trim();
        String gender = request.getOrDefault("gender", "other");
        String location = request.getOrDefault("location", "").trim();
        String contactPhone=request.getOrDefault("contactPhone", "").trim();
        String contactEmail=request.getOrDefault("contactEmail", "").trim();
        boolean shareContact=Boolean.parseBoolean(request.getOrDefault("shareContact","false"));
        if((!contactPhone.isEmpty()&&!contactPhone.matches("[+0-9() -]{3,40}")) || contactEmail.length()>320 || (!contactEmail.isEmpty()&&!contactEmail.matches("[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]+")))
            return ResponseEntity.badRequest().body(Map.of("success",false,"message","请检查联系电话或联系邮箱格式"));
        if (name.isEmpty() || name.length() > 100 || location.length() > 1000
                || !("M".equals(gender) || "F".equals(gender) || "other".equals(gender))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "请检查姓名、性别和地区是否填写正确"));
        }
        User user = userService.getUserByUUID(uuid);
        if (user.getUuid() == null) return ResponseEntity.status(401).body(Map.of("success", false, "message", "账号不存在"));
        jdbcTemplate.update("UPDATE users SET gender=? WHERE uuid=?", gender, uuid);
        jdbcTemplate.update("INSERT INTO account_profiles(uuid,name,location) VALUES (?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),location=VALUES(location)",uuid,name,location);
        if(request.containsKey("contactPhone")||request.containsKey("contactEmail")||request.containsKey("shareContact"))
            jdbcTemplate.update("INSERT INTO account_contacts(uuid,contact_phone,contact_email,share_contact) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE contact_phone=VALUES(contact_phone),contact_email=VALUES(contact_email),share_contact=VALUES(share_contact)",uuid,contactPhone,contactEmail,shareContact);
        if ("family".equals(user.getUserType())) {
            jdbcTemplate.update("UPDATE families SET name=? WHERE uuid=?", name, uuid);
        } else if ("rescuer".equals(user.getUserType())) {
            jdbcTemplate.update("UPDATE rescuers SET name=?, location=? WHERE uuid=?", name, location, uuid);
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "资料已保存"));
    }

    @PostMapping("/password")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request, HttpSession session) {
        String uuid = (String) roleAccess.current(session).get("uuid");
        String current = request.get("currentPassword");
        String next = request.get("newPassword");
        if (next == null || next.length() < 8 || next.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "新密码至少 8 位，最多 72 字节"));
        }
        java.util.List<String> hashes = jdbcTemplate.queryForList("SELECT password FROM users WHERE uuid=? FOR UPDATE", String.class, uuid);
        if (hashes.isEmpty() || current == null || !passwordEncoder.matches(current, hashes.get(0))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "当前密码不正确"));
        }
        jdbcTemplate.update("UPDATE users SET password=? WHERE uuid=?", passwordEncoder.encode(next), uuid);
        jdbcTemplate.update("INSERT INTO account_profiles(uuid,auth_version) VALUES (?,1) ON DUPLICATE KEY UPDATE auth_version=auth_version+1",uuid);
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "密码已修改，请重新登录"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已退出登录");
        return ResponseEntity.ok(result);
    }
}
