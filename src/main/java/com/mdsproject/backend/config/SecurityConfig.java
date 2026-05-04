package com.mdsproject.backend.config;

import com.mdsproject.backend.security.CustomUserDetailsService; 
import com.mdsproject.backend.security.JwtAuthFilter;
import com.mdsproject.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    // THIS is the line the compiler was complaining it couldn't find
    private final CustomUserDetailsService customUserDetailsService; 

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http
                .getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService) 
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    
    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // 1. ADD THIS: Tell Spring Security to integrate with your CorsConfig class
            .cors() 
            .and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
            // 2. ADD THIS: Explicitly allow all OPTIONS requests so the browser's preflight check doesn't get blocked looking for a JWT
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
            // Your existing rules:
            .requestMatchers("/api/auth/**", "/actuator/**", "/health").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthFilter(jwtService, customUserDetailsService), UsernamePasswordAuthenticationFilter.class);

    return http.build();}
}