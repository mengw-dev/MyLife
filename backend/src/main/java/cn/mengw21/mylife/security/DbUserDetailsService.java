package cn.mengw21.mylife.security;

import cn.mengw21.mylife.module.auth.entity.User;
import cn.mengw21.mylife.module.auth.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 从数据库加载用户
 * <p>
 * Spring Security 登录过程中唯一的数据来源。
 * 查不到用户 / 用户已注销停用时抛 {@link UsernameNotFoundException}，
 * DaoAuthenticationProvider 会把它统一转换成"用户名或密码错误"，避免暴露用户是否存在。
 */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
            Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
        // 不存在或已注销/停用 → 统一提示，防用户名枚举
        if (user == null || !Integer.valueOf(User.STATUS_ACTIVE).equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // V1 不实现 RBAC，权限集合为空
        return new LoginUser(user.getId(), user.getUsername(), user.getPasswordHash(), Collections.emptyList());
    }
}