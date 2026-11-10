package com.example.petcommunity3.service;
import com.example.petcommunity3.dto.LoginDTO;
import com.example.petcommunity3.dto.RegisterDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.vo.UserProfileVO;



public interface UserService {
    // 注册用户：接收注册 DTO，校验用户名和密码，再保存到 sys_user 表。
    void register(RegisterDTO dto);

    // 用户登录：校验用户名、密码和账号状态，成功后返回不包含密码的 User。
    User login(LoginDTO dto);

    // 用户个人主页：组合用户基础信息、关注数据、宠物数据和最近帖子。
    UserProfileVO getProfile(Long userId, Long viewerId);

}
