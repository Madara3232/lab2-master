package com.example.lab1.security;

import com.example.lab1.repository.AccountRepository;
import com.example.lab1.service.AccountService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmail(email)
                .map(AccountPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Authentication failed"));
    }

    @Bean
    ApplicationRunner bootstrapAdmin(AccountService accounts,
                                    @Value("${ADMIN_EMAIL:}") String email,
                                    @Value("${ADMIN_PASSWORD:}") String password) {
        return args -> accounts.bootstrapAdmin(email, password);
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, error) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Lab 3-4\", charset=\"UTF-8\"");
            writeError(response, 401, "AUTHENTICATION_REQUIRED", "Valid credentials required");
        };
        AccessDeniedHandler denied = (request, response, error) -> {
            if (error instanceof CsrfException) {
                writeError(response, 403, "CSRF_INVALID", "A valid CSRF token is required");
            } else {
                writeError(response, 403, "ACCESS_DENIED", "Access denied");
            }
        };

        // Only the CSRF token uses a session. Basic credentials are verified on each request.
        var requestContext = new RequestAttributeSecurityContextRepository();
        return http
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized)
                        .securityContextRepository(requestContext))
                .sessionManagement(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(requestContext))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized).accessDeniedHandler(denied))
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**", "/hello", "/number").authenticated()
                        .requestMatchers("/api/**").hasAnyRole("EDITOR", "ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    private static void writeError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":%s,"code":"%s","message":"%s"}
                """.formatted(status, code, message));
    }
}
