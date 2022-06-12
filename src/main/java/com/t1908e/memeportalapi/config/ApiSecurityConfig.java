package com.t1908e.memeportalapi.config;

import com.t1908e.memeportalapi.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationService authenticationService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //override default login path
        com.t1908e.memeportalapi.config.ApiAuthenticationFilter apiAuthenticationFilter
                = new com.t1908e.memeportalapi.config.ApiAuthenticationFilter(
                authenticationManagerBean(), authenticationService);
        apiAuthenticationFilter.setFilterProcessesUrl("/api/v1/login");
        http.cors().disable();
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests()
                .antMatchers("/api/v1/register**", "/api/v1/register/*",
                        "/api/v1/login**", "/api/v1/login/*",
                        "/api/v1/token/refresh**", "/api/v1/token/refresh/*")
                .permitAll();
        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/api/v1/posts/**", "/api/v1/users/**", "/api/v1/posts**",
                        "/api/v1/advertisements/active**", "/api/v1/categories**", "/api/v1/categories/*")
                .permitAll();
        http.authorizeRequests().antMatchers("/api/v1/posts/verify",
                "/api/v1/posts/verify/*").hasAnyAuthority("admin");
        http.authorizeRequests().antMatchers(HttpMethod.DELETE,
                "/api/v1/posts**", "/api/v1/posts/*",
                "/api/v1/users/**")
                .hasAnyAuthority("admin");
        http.authorizeRequests().antMatchers(HttpMethod.PUT, "/api/v1/posts**",
                "/api/v1/posts/**",
                "/api/v1/posts/{id}/makeNew/**", "/api/v1/posts/{id}/makeNew**",
                "/api/v1/posts/{id}/makeHot/**", "/api/v1/posts/{id}/makeHot**"
                , "/api/v1/tokens/giveToken/**", "/api/v1/tokens/giveToken**")
                .hasAnyAuthority("admin");
        http.authorizeRequests()
                .antMatchers(HttpMethod.POST,
                        "/api/v1/posts/**", "/api/v1/posts*", "/api/v1/categories/**", "/api/v1/categories*")
                .hasAnyAuthority("user", "admin");
        //add requests path for more role here
        http.authorizeRequests().antMatchers("/api/v1/tokens/**", "/api/v1/tokens*",
                "/api/v1/reports/**", "/api/v1/reports*",
                "/api/v1/advertisements/**", "/api/v1/advertisements*",
                "/api/v1/transactions/**", "/api/v1/transactions*",
                "/api/v1/dashboard/**", "/api/v1/dashboard*")
                .hasAnyAuthority("user", "admin");
        http.authorizeRequests().anyRequest().authenticated();
        http.addFilter(apiAuthenticationFilter);
        http.addFilterBefore(new CorsFilter(), ChannelProcessingFilter.class);
        http.addFilterBefore(new ApiAuthorizationFilter(authenticationService), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}
