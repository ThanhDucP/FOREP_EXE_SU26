package com.forep.exe.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final RequestMatcher PUBLIC_REQUESTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/public/subscription-plans", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/public/subscription-plans/*", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/public/workspace-registrations", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/public/workspace-registrations/*", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/public/workspace-registrations/*/select-plan", HttpMethod.PATCH.name()),
            new AntPathRequestMatcher("/api/public/workspace-registrations/*/cancel", HttpMethod.PATCH.name()),
            new AntPathRequestMatcher("/api/public/workspace-registrations/*/payments", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/public/payments/*/status", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/public/payment-files/*", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/payment-callbacks/momo", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/payment-callbacks/bank", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/api/v1/health", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/api/v1/auth/login", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**")
    );
    private final JwtService jwtService;
    private final AuthorizationService authorizationService;

    public JwtAuthenticationFilter(JwtService jwtService, AuthorizationService authorizationService) {
        this.jwtService = jwtService;
        this.authorizationService = authorizationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || PUBLIC_REQUESTS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwtService.parse(authorization.substring(7)).ifPresent(user -> {
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                authorizationService.permissionsFor(user.role()).stream()
                        .map(AuthorizationService::authority)
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
