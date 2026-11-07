package cn.mengw21.mylife.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 登录用户主体
 * <p>
 * 实现 {@link UserDetails}，登录成功后由 Spring Security 放入
 * {@code SecurityContextHolder}，业务层通过 {@link CurrentUser} 获取。
 * 第二阶段 auth 模块的 {@code UserDetailsService} 负责构造并返回它。
 */
@Getter
public class LoginUser implements UserDetails {

    /** 用户主键 */
    private final Long id;

    /** 登录名 */
    private final String username;

    /** BCrypt 密文（不会返回给前端） */
    private final String password;

    /** 权限集合（第一阶段留空，RBAC 后续再说） */
    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUser(Long id, String username, String password,
                     Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities == null ? Collections.emptyList() : authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
