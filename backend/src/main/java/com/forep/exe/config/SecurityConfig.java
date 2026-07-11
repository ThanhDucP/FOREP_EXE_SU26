package com.forep.exe.config;

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
                        .requestMatchers("/api/public/**", "/api/payment-callbacks/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/workspace-registrations").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/momo/callback", "/api/v1/payments/bank-transfer/callback").permitAll()
                        .requestMatchers("/api/v1/health", "/api/v1/auth/login", "/api/v1/workspaces/register",
                                "/api/v1/subscription-plans", "/api/v1/subscription-plans/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAnyRole("PLATFORM_ADMIN", "SYSTEM_ADMIN")
                        .requestMatchers("/api/v1/employees/**", "/api/v1/hr/**", "/api/workspace/hr/**").hasAnyRole("BUSINESS_OWNER", "OWNER", "HR")
                        .requestMatchers("/api/v1/analytics/**", "/api/v1/ai/**", "/api/workspace/ai/**", "/api/workspace/workload/**", "/api/workspace/business-owner/**").hasAnyRole("BUSINESS_OWNER", "MANAGER", "OWNER")
                        .requestMatchers("/api/workspace/tasks/**").hasAnyRole("BUSINESS_OWNER", "MANAGER", "EMPLOYEE", "OWNER")
                        .requestMatchers("/api/v1/workspaces/current").hasAnyRole("BUSINESS_OWNER", "OWNER", "HR", "MANAGER", "EMPLOYEE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
