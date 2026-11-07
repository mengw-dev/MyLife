package cn.mengw21.mylife.module.auth.service;

import cn.mengw21.mylife.module.auth.dto.ChangePasswordRequest;
import cn.mengw21.mylife.module.auth.dto.LoginRequest;
import cn.mengw21.mylife.module.auth.dto.RegisterRequest;
import cn.mengw21.mylife.module.auth.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证服务（对齐《02-API接口文档》第 2 节）
 * <p>
 * 所有身份判断都以服务端 Session 中的登录态为准，不信任客户端传参。
 */
public interface AuthService {

    /**
     * 注册：创建用户（用户名唯一、密码 BCrypt 存储）
     *
     * @param request 注册请求
     * @return 新用户信息
     */
    UserVO register(RegisterRequest request);

    /**
     * 登录：校验用户并把认证信息写入 Session；成功后刷新 last_login_at
     *
     * @param request  登录请求
     * @param httpRequest  HTTP 请求（用于写 Session）
     * @param httpResponse HTTP 响应
     * @return 登录用户信息
     */
    UserVO login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 登出：清理 SecurityContext 并使 Session 失效
     */
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser();

    /**
     * 修改密码：校验原密码，成功后注销当前会话要求重新登录
     */
    void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 注销账号：清理当前用户的全部业务数据与媒体文件后删除账号，并使会话失效
     */
    void deleteAccount(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}