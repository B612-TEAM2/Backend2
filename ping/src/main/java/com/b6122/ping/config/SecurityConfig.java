package com.b6122.ping.config;

import com.b6122.ping.config.jwt.JwtAuthorizationFilter;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final UserDetailsService userDetailsService;
    private final UserDataRepository userDataRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        //AuthenticaionManager 생성
        AuthenticationManagerBuilder sharedObject = http.getSharedObject(AuthenticationManagerBuilder.class);
        sharedObject.userDetailsService(this.userDetailsService); //이 userDetailsService와 PrincipalDetailsService에서 상속받는 인터페이스는 서로 같음.
        AuthenticationManager authenticationManager = sharedObject.build();
        http.authenticationManager(authenticationManager);
        //세션 만들지 않기.
        http
                .addFilter(corsConfig.corsFilter())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin((formLogin) -> formLogin.disable())
//                .addFilter(new JwtAuthenticationFilter((authenticationManager)))
                .addFilter((new JwtAuthorizationFilter(authenticationManager, userDataRepository)))
                .httpBasic((httpBasic) -> httpBasic.disable()) //Bearer 방식을 사용하기 위해 basic 인증 비활성화
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .requestMatchers("/api/oauth/jwt/**").permitAll()
                                .requestMatchers("/admin/**").hasAnyRole("ADMIN")
                                .requestMatchers("/**").hasAnyRole("ADMIN", "USER")
                                .anyRequest().authenticated());


        return http.build();
    }
}
