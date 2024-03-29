package com.b6122.ping.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.b6122.ping.dto.OauthLoginUserDto;
import com.b6122.ping.dto.UserDto;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtProvider {

    /**
     * 사용자 정보를 바탕으로 Jwt AccessToken 발급
     * @param oauthLoginDto UserDto 정보: id, username, wasMember
     * @return
     */
    public Map<String, Object> createJwtAccessAndRefreshToken(OauthLoginUserDto oauthLoginDto) {

        //accessToken 생성
        String accessToken = JWT.create()
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtProperties.ACCESS_TOKEN_EXPIRATION_TIME))
                .withClaim("id", oauthLoginDto.getId())
                .withClaim("username", oauthLoginDto.getUsername())
                .withClaim("token_type", "access")
                .withClaim("expires-at", JwtProperties.ACCESS_TOKEN_EXPIRATION_TIME)
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        //refreshToken 생성
        String refreshToken = JWT.create()
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtProperties.REFRESH_TOKEN_EXPIRATION_TIME))
                .withClaim("id", oauthLoginDto.getId())
                .withClaim("username", oauthLoginDto.getUsername())
                .withClaim("token_type", "refresh")
                .withClaim("expires-at", JwtProperties.REFRESH_TOKEN_EXPIRATION_TIME)
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        //responseBody에 값 저장해서 return
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("access-token", accessToken);
        responseBody.put("refresh-token", refreshToken);

        return responseBody;
    }

    public Map<String, String> createJwtAccessToken(UserDto userDto) {

        //accessToken 생성
        String accessToken = JWT.create()
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtProperties.ACCESS_TOKEN_EXPIRATION_TIME))
                .withClaim("id", userDto.getId())
                .withClaim("username", userDto.getUsername())
                .withClaim("token_type", "access")
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        //responseBody에 값 저장해서 return
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("access-token", accessToken);

        return responseBody;
    }
}
