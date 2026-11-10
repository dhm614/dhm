package com.example.petcommunity3.controller;


import com.example.petcommunity3.service.UserService;
import com.example.petcommunity3.common.Result;
import com.example.petcommunity3.dto.LoginDTO;
import com.example.petcommunity3.dto.RegisterDTO;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.vo.UserProfileVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    // Controller 只负责接收 HTTP 请求和返回统一结果，具体注册、登录、主页查询逻辑交给 UserService。
    private final UserService userService;

    // 构造方法注入：Spring 创建 UserController 时，会把 UserService 的实现类自动传进来。
    public UserController(UserService userService){

        this.userService = userService;
    }

    // POST /api/users/register
    // @RequestBody 会把前端传来的 JSON 转成 RegisterDTO，再交给 Service 完成注册。
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto){
        userService.register(dto);
        return Result.success(null);
    }

    // POST /api/users/login
    // 登录成功后返回 User 对象，Service 中会把 password 置空，避免把密码返回给前端。
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginDTO dto){
        User user = userService.login(dto);
        return Result.success(user);
    }

    // GET /api/users/{id}/profile
    // @PathVariable 读取路径里的用户 id；viewerId 是可选参数，用来判断当前访问者是否关注了这个用户。
    @GetMapping("/{id}/profile")
    public Result<UserProfileVO> profile(@PathVariable Long id,
                                         @RequestParam(required = false) Long viewerId) {
        return Result.success(userService.getProfile(id, viewerId));
    }
}
