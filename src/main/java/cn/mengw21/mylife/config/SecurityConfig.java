package cn.mengw21.mylife.config;

import cn.mengw21.mylife.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Spring Security 基础配置
 * <p>
 * 策略：Session 登录认证 + BCrypt 密码编码 + CSRF Cookie 模式（对齐《04-项目开发规范》第 5 节）
 * <ul>
 *     <li>匿名可访问：健康检查、CSRF 端点、注册、登录、API 文档</li>
 *     <li>其余所有接口都需要认证</li>
 *     <li>未登录 → 401 JSON；已登录但无权限 → 403 JSON（业务 code 40101 / 40301）</li>
 *     <li>CSRF：前端从 {@code XSRF-TOKEN} Cookie 取值放入 {@code X-XSRF-TOKEN} 请求头</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 免认证路径（对齐《02-API接口文档》第 2 节 + API 文档页面）
     */
    private static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/error",
        "/favicon.ico",
        "/api/v1/auth/csrf",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        // OpenAPI / Swagger UI（接口契约页面，只读）
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository,
                                           SessionRegistry sessionRegistry) throws Exception {
        http
            // 跨域：交给 CorsConfig 中的规则处理
            .cors(Customizer.withDefaults())

            // CSRF：Cookie 模式（前端从 XSRF-TOKEN Cookie 取值，放到 X-XSRF-TOKEN 请求头）
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )

            // 不使用默认的 formLogin / httpBasic — 全部返回 JSON
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // 认证信息持久化到 Session（登录接口手动写 SecurityContext 时用同一 repository）
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))

            // Session：防固定攻击 + 单账号单会话
            .sessionManagement(session -> {
                session.sessionFixation().migrateSession();
                session.maximumSessions(1).sessionRegistry(sessionRegistry);
            })

            // 请求授权
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
            )

            // 异常：401 / 403 统一 JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResponse.UNAUTHORIZED, "未登录或登录已过期"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeJson(response, HttpStatus.FORBIDDEN.value(), ApiResponse.FORBIDDEN, "没有访问权限"))
            );

        return http.build();
    }

    /**
     * 认证管理器：DaoAuthenticationProvider 组合我们的 UserDetailsService 与 BCrypt
     */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * SecurityContext 存储方式 —— Session。
     * 登录接口手动完成认证后，要用它把 SecurityContext 写入 Session，
     * Spring Security 6 不再自动保存。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 会话注册表 —— {@code maximumSessions(1)} 靠它统计登录会话数
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 让 HttpSession 事件发布到 Spring，SessionRegistry 才能感知会话销毁
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * BCrypt 密码编码器 — 注册加密、登录校验统一使用
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 把认证失败原因写成统一响应体（对齐全局响应格式）
     */
    private static void writeJson(HttpServletResponse response, int httpStatus, int code, String message)
        throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}