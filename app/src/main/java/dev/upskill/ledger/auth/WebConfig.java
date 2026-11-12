package dev.upskill.ledger.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenStore tokens;

    public WebConfig(TokenStore tokens) {
        this.tokens = tokens;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/api/**", "/css/**", "/js/**", "/favicon.svg", "/error");
        registry.addInterceptor(new ApiAuthInterceptor(tokens))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/health", "/api/testing/**");
    }
}
