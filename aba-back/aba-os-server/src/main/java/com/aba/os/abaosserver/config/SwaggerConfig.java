package com.aba.os.abaosserver.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("ABA-OS API Server")
                .version("v1.2")
                .description("발달장애 아동 치료 센터 관리 시스템 API\n\n" +
                        "## 인증 방법\n" +
                        "1. `/api/v1/auth/login` 엔드포인트로 로그인하여 accessToken을 발급받습니다.\n" +
                        "2. 우측 상단의 'Authorize' 버튼을 클릭합니다.\n" +
                        "3. 발급받은 accessToken을 입력합니다 (Bearer 접두사 없이).\n" +
                        "4. 이후 모든 API 요청에 자동으로 JWT 토큰이 포함됩니다.")
                .contact(new Contact()
                        .name("ABA-OS Team")
                        .email("support@aba-os.com"))
                .license(new License()
                        .name("Private License")
                        .url("https://aba-os.com/license"));
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT Access Token을 입력하세요. (Bearer 접두사 없이 토큰만 입력)");
    }
}
