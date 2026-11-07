package cn.mengw21.mylife.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置
 * <p>
 * 接口文档页：{@code /swagger-ui.html}、契约 JSON：{@code /v3/api-docs}。
 * 认证方式声明为 Session Cookie（{@code MYLIFE_SESSION}），与 SecurityConfig 一致。
 * 所有 Controller 使用 {@code @Tag / @Operation} 注解补充分组与描述。
 */
@Configuration
public class OpenApiConfig {

    /** Session Cookie 名称（与 application.yaml 中 server.servlet.session.cookie.name 一致） */
    private static final String SESSION_COOKIE_NAME = "MYLIFE_SESSION";

    /** 安全方案注册名 */
    private static final String SECURITY_SCHEME_NAME = "session";

    @Bean
    public OpenAPI myLifeOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("MyLife API")
                .description("MyLife 个人生活与知识成长记录系统接口契约（V1.0）。"
                    + "除注册 / 登录 / CSRF 外，所有接口需要登录（Session Cookie 认证）。")
                .version("1.0")
                .contact(new Contact().name("MyLife Team")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name(SESSION_COOKIE_NAME)));
    }
}