//package com.b6122.ping.config.jwt;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.b6122.ping.auth.PrincipalDetails;
//import com.b6122.ping.domain.User;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import java.io.IOException;
//import java.util.Date;
//
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
//
//    private final AuthenticationManager authenticationManager;
//
//    //로그인 요청을 하면, 로그인 시도를 위해 실행되는 메소드
//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
//        System.out.println("로그인 시도중");
//
//        //넘어온 username과 pw를 받아서
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            //json 데이터를 가정
//            User user = objectMapper.readValue(request.getInputStream(), User.class);
//            //authenticationManager에 넘겨주기 위해 토큰 생성
//            UsernamePasswordAuthenticationToken authenticationToken =
//                    new UsernamePasswordAuthenticationToken(user.getUsername(), "1234");
//            //토큰을 authenticate()에 넘겨주면, PrincipalDetailsService의 loadByUsername() 실행
//            //service에서 인증이 완료되면 로그인 완료(DB의 데이터와 들어온 데이터가 일치)
//            Authentication authentication = authenticationManager.authenticate(authenticationToken);
//
//            return authentication; //세션에 저장
//            //세션에 저장하는 이유는, security가 권한 관리를 해주기 때문에
//            //자세한 이유 --> https://www.inflearn.com/questions/714149/jwt-token-%EA%B5%AC%ED%98%84%EC%97%90%EC%84%9C-session-%EC%9D%84-%EC%82%AC%EC%9A%A9%ED%95%9C%EB%8B%A4
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    // attemptAuthentication이 성공적으로 실행 이후 successfulAuthentication 메소드 실행.
//    // 아래 메소드에서 JWT 토큰 생성 후 사용자에게 response 해준다.
//    @Override
//    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
//        PrincipalDetails principalDetails = (PrincipalDetails) authResult.getPrincipal();
//
//        //HMAC512 방식 암호화
//        String jwtToken = JWT.create()
//                .withSubject("jwt토큰")
//                .withExpiresAt(new Date(System.currentTimeMillis() + (60000 * 10))) //10분
//                .withClaim("id", principalDetails.getUser().getId())
//                .withClaim("username",principalDetails.getUser().getUsername())
//                .sign(Algorithm.HMAC512("secretKeyByServer"));
//        response.addHeader("Authorization", "Bearer " + jwtToken);
//    }
//}
