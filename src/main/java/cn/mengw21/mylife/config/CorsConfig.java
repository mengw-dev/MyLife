package cn.mengw21.mylife.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨域配置
 * <p>
 * 前后端分离开发时（Vue 跑在 5173，后端跑在 8080），浏览器会拦截跨域请求。
 * 这里只放行本机前端开发服务器，生产同域部署时不会触发。
 * <p>
 * 以 {@link CorsConfigurationSource} Bean 的形式提供，Spring Security 的
 * {@code .cors(withDefaults())} 能直接拿到它，不需要回退去猜 MVC 的配置。
 * <p>
 * 注意：{@code allowCredentials(true)} 不能和 {@code allowedOrigins("*")} 共用，
 * 必须用 {@code allowedOriginPatterns}，否则浏览器会拒绝携带 Cookie。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
