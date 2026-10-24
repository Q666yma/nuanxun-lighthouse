package com.maka.mapper;

import com.maka.pojo.User;
import com.maka.pojo.Rescuer;
import com.maka.pojo.Family;
import com.maka.query.UserInfo;
import com.maka.vo.RescuingUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.List;

/**
 */

//

@Mapper
public interface UserMapper {
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 存在返回1，不存在返回0
     */
    int checkUsernameExists(String username);
    
    /**
     * 检查手机号是否存在
     * @param phone 手机号
     * @return 存在返回1，不存在返回0
     */
    int checkPhoneExists(String phone);
    
    /**
     * 插入新用户
     * @param user 用户对象
     * @return 影响行数
     */
    int insertUser(User user);
    
    /**
     * 插入搜救志愿者
     * @param rescuer 志愿者对象
     * @return 影响行数
     */
    int insertRescuer(Rescuer rescuer);
    
    /**
     * 插入家属
     * @param family 家属对象
     * @return 影响行数
     */
    int insertFamily(Family family);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User getUserByUsername(String username);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户对象
     */
    User getUserByPhone(String phone);
    
    /**
     * Á根据UUID查询用户
     * @param uuid UUID
     * @return 用户对象
     */
    User getUserByUuid(String uuid);
    
    /**
     * 根据UUID查询志愿者
     * @param uuid UUID
     * @return 志愿者对象
     */
    Rescuer getRescuerByUuid(String uuid);
    
    /**
     * 根据UUID查询家属
     * @param uuid UUID
     * @return 家属对象
     */
    Family getFamilyByUuid(String uuid);
    
    /**
     * 更新用户状态
     * @param uuid UUID
     * @param status 状态
     * @return 影响行数
     */
    int updateUserStatus(@Param("uuid") String uuid, @Param("status") String status);
    
    /**
     * 更新志愿者状态
     * @param uuid UUID
     * @param status 状态
     * @return 影响行数
     */
    int updateRescuerStatus(@Param("uuid") String uuid, @Param("status") String status);
}