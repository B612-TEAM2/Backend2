package com.b6122.ping.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.b6122.ping.auth.PrincipalDetails;
import com.b6122.ping.domain.User;
import com.b6122.ping.dto.UserDto;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import com.b6122.ping.service.JwtService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private UserDataRepository userDataRepository;
    private JwtService jwtService;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, UserDataRepository userDataRepository) {
        super(authenticationManager);
        this.userDataRepository = userDataRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader(JwtProperties.HEADER_STRING);

        if (header == null || !header.startsWith(JwtProperties.TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        //토큰 검증(동일한 토큰인지, 만료시간은 안지났는지 com.auth0.jwt 라이브러리가 확인해줌)
        //access token과 refresh token을 검증
        String username = null;
        String token = request.getHeader(JwtProperties.HEADER_STRING).replace(JwtProperties.TOKEN_PREFIX, "");
        String tokenType = JWT.decode(token).getClaim("token_type").asString();

        if (tokenType.equals("access")) {
            try {
                username = JWT.require(Algorithm.HMAC512(JwtProperties.SECRET)).build().verify(token).getClaim("username").asString();
            } catch(JWTVerificationException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Access token is not valid. Please send refersh token.");
                return;
            }
        } else if (tokenType.equals("refresh")) {

            try {
                username = JWT.require(Algorithm.HMAC512(JwtProperties.SECRET)).build().verify(token).getClaim("username").asString();
                User user = userDataRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
                UserDto userDto = new UserDto(user.getId(), user.getUsername());
                Map<String, String> jwtAccessToken = jwtService.createJwtAccessToken(userDto);

                //refersh token이 유효하면 access token 새로 발급
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"access-token\": \"" + jwtAccessToken.get("access-token") + "\"}");
                return;

            } catch (JWTVerificationException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Refresh token is not valid. Please login again.");
                return;
            }
        }

        if (username != null) {
            User user = userDataRepository.findByUsername(username).orElseThrow(EntityNotFoundException::new);

            // 인증은 토큰 검증시 끝. 인증을 하기 위해서가 아닌 스프링 시큐리티가 수행해주는 권한 처리를 위해
            // 아래와 같이 토큰을 만들어서 Authentication 객체를 강제로 만들고 그걸 세션에 저장
            PrincipalDetails principalDetails = new PrincipalDetails(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails,
                    null,
                    principalDetails.getAuthorities());
            // 강제로 시큐리티의 세션에 접근하여 값 저장
            // -> 컨트롤러에서 Authentication 객체를 받아서(DI) 권한 처리를 할 수 있음.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }

}
