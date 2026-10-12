package com.smartparking.backend.config;

import com.smartparking.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty() || origins.contains("*")) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(origins);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Please login to continue\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"You do not have permission to do this\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()

                // Administrator only
                .requestMatchers("/api/admin/**", "/api/reports/**").hasRole("ADMIN")
                .requestMatchers(
                    "/api/parking-analytics/**",
                    "/api/parking-booking-status-analytics/**",
                    "/api/parking-booking-vehicle-analytics/**",
                    "/api/parking-location-analytics/**",
                    "/api/parking-revenue/**",
                    "/api/parking-role-analytics/**",
                    "/api/parking-session-analytics/**",
                    "/api/parking-slot-types/**",
                    "/api/parking-user-analytics/**",
                    "/api/parking-vehicle-analytics/**"
                ).hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/payments").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/payments/*/verify").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/notifications").hasRole("ADMIN")
                .requestMatchers("/api/notifications/create/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/parking-permissions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/parking-permissions/status/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/parking-permissions/*/status").hasRole("ADMIN")

                // Security guards and administrators
                .requestMatchers(
                    "/api/security-dashboard/**",
                    "/api/security-activity/**",
                    "/api/parking-violations/**",
                    "/api/parking-alerts/**",
                    "/api/parking-events/**",
                    "/api/vehicle-duplicates/**"
                ).hasAnyRole("ADMIN", "GUARD")
                .requestMatchers(HttpMethod.POST, "/api/qr/scan").hasAnyRole("ADMIN", "GUARD")
                .requestMatchers("/api/parking-sessions/**").hasAnyRole("ADMIN", "GUARD")
                .requestMatchers(HttpMethod.GET, "/api/bookings").hasAnyRole("ADMIN", "GUARD")
                .requestMatchers(HttpMethod.GET, "/api/vehicles").hasAnyRole("ADMIN", "GUARD")
                .requestMatchers(HttpMethod.GET, "/api/parking-passes").hasAnyRole("ADMIN", "GUARD")
                .requestMatchers(HttpMethod.PUT, "/api/bookings/*/approve", "/api/bookings/*/reject").hasRole("ADMIN")

                // Everybody who is logged in can view locations and slots; only admins change them
                .requestMatchers(HttpMethod.GET, "/api/parking-locations/**", "/api/parking-slots/**").authenticated()
                .requestMatchers("/api/parking-locations/**", "/api/parking-slots/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
