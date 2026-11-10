package com.example.petcommunity3.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private Integer adminLevel;
    private String adminScope;
    private Integer isMerchant;
    private Integer merchantApplyStatus;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
