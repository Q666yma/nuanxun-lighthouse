// package com.maka.query;

// import com.maka.service.UserService;
// import lombok.Data;

// import javax.validation.constraints.*;

// /**
//  * 队员注册的参数
//  */
// @Data
// public class SimpleUser {

//     @NotNull(message = "队员名不能为空")
//     @NotEmpty(message = "队员名不能为空")
//     private String userName;
//     @NotNull(message = "性别不能为空")
//     @NotEmpty(message = "性别不能为空")
//     private String userGender;

//     @NotBlank(message = "手机号码不能为空")
//     @Size(min = 11, max = 11, message = "手机号码长度不正确")
//     @Pattern(regexp = "^(((13[0-9])|(14[579])|(15([0-3]|[5-9]))|(16[6])|(17[0135678])|(18[0-9])|(19[89]))\\d{8})$", message = "手机号格式错误")
//     private String userPhone;

//     private String desc;

//     private Boolean worker = true;





// }

