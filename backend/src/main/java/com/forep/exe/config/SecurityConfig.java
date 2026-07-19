package com.forep.exe.config;

import com.forep.exe.domain.Enums.Permission;
import com.forep.exe.security.AuthorizationService;
import com.forep.exe.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/subscription-plans", "/api/public/subscription-plans/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/workspace-registrations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/workspace-registrations/*", "/api/public/payments/*/status",
                                "/api/public/payment-files/*").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/public/workspace-registrations/*/select-plan",
                                "/api/public/workspace-registrations/*/cancel").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/workspace-registrations/*/payments",
                                "/api/payment-callbacks/momo", "/api/payment-callbacks/bank").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/auth/login",
                                "/api/v1/subscription-plans", "/api/v1/subscription-plans/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/payments/*/confirm", "/api/v1/admin/payments/*/confirm",
                                "/api/v1/admin/workspace-registrations/*/confirm-payment").hasAuthority(permission(Permission.PAYMENT_CONFIRM))
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/payments/*/reject", "/api/v1/admin/payments/*/reject").hasAuthority(permission(Permission.PAYMENT_CONFIRM))
                        .requestMatchers(HttpMethod.PUT, "/api/admin/payment-qr-settings/*").hasAuthority(permission(Permission.PAYMENT_QR_MANAGE))
                        .requestMatchers(HttpMethod.POST, "/api/admin/payment-qr-settings/*/qr-image").hasAuthority(permission(Permission.PAYMENT_QR_MANAGE))
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/payment-qr-settings/*/qr-image").hasAuthority(permission(Permission.PAYMENT_QR_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/api/admin/payment-qr-settings").hasAuthority(permission(Permission.PAYMENT_QR_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/api/admin/payments/**", "/api/v1/payments/**").hasAuthority(permission(Permission.PAYMENT_HISTORY_VIEW))
                        .requestMatchers("/api/admin/subscription-plans/**", "/api/v1/admin/subscription-plans/**").hasAuthority(permission(Permission.PACKAGE_MANAGE))
                        .requestMatchers("/api/admin/workspace-registrations/**", "/api/admin/workspaces/**",
                                "/api/v1/admin/workspace-registrations/**", "/api/v1/admin/workspaces/**",
                                "/api/v1/workspace-registrations/**").hasAuthority(permission(Permission.WORKSPACE_MANAGE))
                        .requestMatchers("/api/admin/business-feedback/**", "/api/v1/admin/business-feedback/**").hasAuthority(permission(Permission.FEEDBACK_MANAGE))
                        .requestMatchers("/api/admin/audit-logs").hasAuthority(permission(Permission.AUDIT_LOG_VIEW))
                        .requestMatchers("/api/admin/dashboard/**", "/api/v1/admin/monitoring").hasAuthority(permission(Permission.REVENUE_VIEW))
                        .requestMatchers("/api/admin/ai/platform-summary").hasAuthority(permission(Permission.AI_SUMMARY))
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAuthority(permission(Permission.SYSTEM_CONFIGURATION))
                        .requestMatchers(HttpMethod.POST, "/api/v1/employees").hasAuthority(permission(Permission.EMPLOYEE_CREATE))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/*").hasAuthority(permission(Permission.EMPLOYEE_UPDATE))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/employees/*/status").hasAuthority(permission(Permission.EMPLOYEE_DEACTIVATE))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/employees/*/reset-password").hasAuthority(permission(Permission.EMPLOYEE_UPDATE))
                        .requestMatchers(HttpMethod.GET, "/api/v1/employees/**").hasAuthority(permission(Permission.EMPLOYEE_VIEW))
                        .requestMatchers(HttpMethod.GET, "/api/workspace/hr/employees/import-template",
                                "/api/workspace/hr/employees/imports/**").hasAuthority(permission(Permission.EMPLOYEE_IMPORT))
                        .requestMatchers(HttpMethod.POST, "/api/workspace/hr/employees/import",
                                "/api/workspace/hr/employees/imports/*/confirm").hasAuthority(permission(Permission.EMPLOYEE_IMPORT))
                        .requestMatchers(HttpMethod.DELETE, "/api/workspace/hr/employees/imports/*").hasAuthority(permission(Permission.EMPLOYEE_IMPORT))
                        .requestMatchers(HttpMethod.GET, "/api/workspace/hr/employees/**").hasAuthority(permission(Permission.EMPLOYEE_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/workspace/hr/employees").hasAuthority(permission(Permission.EMPLOYEE_CREATE))
                        .requestMatchers(HttpMethod.PUT, "/api/workspace/hr/employees/*").hasAuthority(permission(Permission.EMPLOYEE_UPDATE))
                        .requestMatchers(HttpMethod.PATCH, "/api/workspace/hr/employees/*/status").hasAuthority(permission(Permission.EMPLOYEE_DEACTIVATE))
                        .requestMatchers(HttpMethod.GET, "/api/workspace/business-owner/hr-accounts").hasAuthority(permission(Permission.HR_ACCOUNT_MANAGE))
                        .requestMatchers(HttpMethod.POST, "/api/workspace/business-owner/hr-accounts").hasAuthority(permission(Permission.HR_ACCOUNT_MANAGE))
                        .requestMatchers(HttpMethod.PATCH, "/api/workspace/business-owner/hr-accounts/*/status").hasAuthority(permission(Permission.HR_ACCOUNT_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/api/v1/hr/departments/**", "/api/workspace/hr/departments/**").hasAuthority(permission(Permission.DEPARTMENT_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/v1/hr/departments", "/api/workspace/hr/departments").hasAuthority(permission(Permission.DEPARTMENT_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/hr/departments/*", "/api/workspace/hr/departments/*").hasAuthority(permission(Permission.DEPARTMENT_MANAGE))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/hr/departments/**", "/api/workspace/hr/departments/**").hasAuthority(permission(Permission.DEPARTMENT_MANAGE))
                        .requestMatchers(HttpMethod.GET, "/api/v1/hr/job-positions/**", "/api/workspace/hr/job-positions/**",
                                "/api/workspace/hr/business-positions/**").hasAuthority(permission(Permission.POSITION_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/v1/hr/job-positions", "/api/workspace/hr/job-positions",
                                "/api/workspace/hr/business-positions").hasAuthority(permission(Permission.POSITION_MANAGE))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/hr/job-positions/*", "/api/workspace/hr/job-positions/*",
                                "/api/workspace/hr/business-positions/*").hasAuthority(permission(Permission.POSITION_MANAGE))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/hr/job-positions/**", "/api/workspace/hr/job-positions/**",
                                "/api/workspace/hr/business-positions/**").hasAuthority(permission(Permission.POSITION_MANAGE))
                        .requestMatchers(HttpMethod.POST, "/api/workspace/tasks", "/api/v1/tasks").hasAuthority(permission(Permission.TASK_CREATE))
                        .requestMatchers(HttpMethod.PUT, "/api/workspace/tasks/*", "/api/v1/tasks/*").hasAuthority(permission(Permission.TASK_ASSIGN))
                        .requestMatchers(HttpMethod.PATCH, "/api/workspace/tasks/*/assign", "/api/workspace/tasks/*/assign-individual",
                                "/api/workspace/tasks/*/assign-team", "/api/v1/tasks/*/assign", "/api/v1/tasks/*/assign-individual",
                                "/api/v1/tasks/*/assign-team").hasAuthority(permission(Permission.TASK_ASSIGN))
                        .requestMatchers(HttpMethod.PATCH, "/api/workspace/tasks/*/approve-completion", "/api/workspace/tasks/*/return",
                                "/api/workspace/tasks/*/cancel", "/api/v1/tasks/*/approve-completion", "/api/v1/tasks/*/return",
                                "/api/v1/tasks/*/cancel").hasAuthority(permission(Permission.TASK_APPROVE))
                        .requestMatchers(HttpMethod.PATCH, "/api/workspace/tasks/*/accept", "/api/workspace/tasks/*/submit-completion",
                                "/api/workspace/tasks/*/customer-info", "/api/workspace/tasks/*/status", "/api/workspace/tasks/*/progress",
                                "/api/v1/tasks/*/accept", "/api/v1/tasks/*/submit-completion", "/api/v1/tasks/*/customer-info",
                                "/api/v1/tasks/*/status", "/api/v1/tasks/*/progress").hasAuthority(permission(Permission.TASK_UPDATE_OWN))
                        .requestMatchers(HttpMethod.POST, "/api/workspace/tasks/*/attachments", "/api/workspace/tasks/*/updates",
                                "/api/v1/tasks/*/attachments", "/api/v1/tasks/*/updates").hasAuthority(permission(Permission.TASK_UPDATE_OWN))
                        .requestMatchers(HttpMethod.GET, "/api/workspace/tasks/**", "/api/v1/tasks/**").hasAuthority(permission(Permission.TASK_VIEW))
                        .requestMatchers("/api/workspace/ai/recommendations/**", "/api/v1/ai/recommend-assignee",
                                "/api/v1/ai/recommend-team-leaders", "/api/v1/ai/recommend-team-members",
                                "/api/v1/ai/recommendations/**").hasAuthority(permission(Permission.AI_RECOMMENDATION))
                        .requestMatchers("/api/workspace/ai/tasks/**", "/api/workspace/ai/workload/risk",
                                "/api/workspace/ai/employee-report", "/api/v1/ai/tasks/**", "/api/v1/ai/workload/risk",
                                "/api/v1/ai/employee-report", "/api/v1/ai/delay-risks", "/api/v1/ai/daily-reports/**").hasAuthority(permission(Permission.AI_ANALYZE))
                        .requestMatchers("/api/workspace/ai/business-owner/**", "/api/workspace/business-owner/**",
                                "/api/v1/analytics/owner-dashboard", "/api/v1/ai/workload-summary",
                                "/api/v1/ai/business-summary/**").hasAuthority(permission(Permission.AI_SUMMARY))
                        .requestMatchers("/api/workspace/ai-history", "/api/v1/ai/action-suggestions",
                                "/api/v1/ai/suggestions/**").hasAuthority(permission(Permission.AI_HISTORY))
                        .requestMatchers("/api/workspace/workload/**", "/api/v1/analytics/workload/**").hasAuthority(permission(Permission.REPORT_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/v1/daily-reports").hasAuthority(permission(Permission.REPORT_SUBMIT))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/daily-reports/*/review").hasAuthority(permission(Permission.REPORT_REVIEW))
                        .requestMatchers(HttpMethod.GET, "/api/v1/daily-reports/**").hasAuthority(permission(Permission.REPORT_VIEW))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/workspaces/current").hasAuthority(permission(Permission.WORKSPACE_UPDATE))
                        .requestMatchers(HttpMethod.GET, "/api/v1/workspaces/current").hasAuthority(permission(Permission.WORKSPACE_VIEW))
                        .requestMatchers(HttpMethod.POST, "/api/v1/business-feedback", "/api/workspace/feedback").hasAuthority(permission(Permission.FEEDBACK_CREATE))
                        .requestMatchers("/api/v1/notifications/**").hasAuthority(permission(Permission.NOTIFICATION_VIEW))
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static String permission(Permission permission) {
        return AuthorizationService.authority(permission);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${forep.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-Workspace-Id",
                "X-Registration-Token",
                "Idempotency-Key"
        ));
        configuration.setExposedHeaders(List.of("Location", "Content-Disposition", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
