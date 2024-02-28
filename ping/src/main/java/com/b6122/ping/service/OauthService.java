package com.b6122.ping.service;

import com.b6122.ping.domain.User;
import com.b6122.ping.domain.UserRole;
import com.b6122.ping.dto.UserDto;
import com.b6122.ping.oauth.provider.GoogleUser;
import com.b6122.ping.oauth.provider.KakaoUser;
import com.b6122.ping.oauth.provider.OAuthProperties;
import com.b6122.ping.oauth.provider.OAuthUser;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OauthService {

    private final UserDataRepository userDataRepository;

    //회원 가입 후 jwt 토큰 발급
    public UserDto join(String server, String code) throws IOException {

        Map<String, Object> userInfo = new HashMap<>();
        if ("kakao".equals(server)) {
            String accessToken = getKakaoAccessToken(code);
            userInfo = getKakaoUserInfo(accessToken);
        } else if ("google".equals(server)) {
            String accessToken = getGoogleAccessToken(code);
            userInfo = getGoogleUserInfo(accessToken);
        }
        return joinOAuthUser(userInfo);
    }
    //프론트에서 전달 받은 인가코드로 카카오 서버로 요청해서 access token 받는 메소드
    //요청 데이터 타입 (Content-type: application/x-www-form-urlencoded;charset=utf-8)

    //네트워크 통신 방법
    //1. HttpURLConnection -> 아래 메소드에서 사용
    //2. RestTemplate(추후 deprecated 예정?)
    //3. HttpClient
    //4. WebClient(Spring WebFlux)
    //5. RestTemplate 기반의 Spring 6.1 RestClient
    public String getKakaoAccessToken(String authorizationCode) throws IOException {
        String tokenEndpoint = "https://kauth.kakao.com/oauth/token";

        // HTTP 연결 설정
        URL url = new URL(tokenEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST"); // post만 가능
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); //필수
        connection.setDoOutput(true); //데이터의 전송을 허용, 기본 값 false

        // 데이터 작성, 요청 시 필요한 필수 요소만 포함
        String requestBody = "grant_type=authorization_code" +
                "&client_id=" + OAuthProperties.KAKAO_CLIENT_ID +
                "&redirect_uri" + OAuthProperties.KAKAO_REDIRECT_URI +
                "&code=" + authorizationCode;

        // 데이터 전송
        try (OutputStream os = connection.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            writer.write(requestBody);
            writer.flush();
        }

        // 응답 처리
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // 성공적으로 access token을 받은 경우
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                // access token 추출 후 컨트롤러로 전달
                return jsonNode.get("access_token").asText();
            }
        } else {
            // 오류가 발생한 경우
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                return errorResponse.toString(); //예외처리 필요..
            }
        }
    }

    public Map<String, Object> getKakaoUserInfo(String accessToken) throws IOException{
        String requestEndpoint = "https://kapi.kakao.com/v2/user/me";

        URL url = new URL(requestEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST"); //get post 둘다 가능
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); //필수
        connection.setRequestProperty("Authorization", "Bearer " + accessToken); //필수
        connection.setDoOutput(true);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // 성공적으로 값을 전송받았다면
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> userInfoMap = objectMapper.readValue(response.toString(), Map.class);
                userInfoMap.put("provider", "kakao");
                return userInfoMap;
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(errorResponse.toString(), Map.class); //예외처리 필요..
            }
        }
    }

    public String getGoogleAccessToken(String authorizationCode) throws IOException {
        String tokenEndpoint = "https://www.googleapis.com/oauth2/v4/token";

        // HTTP connection setup
        URL url = new URL(tokenEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        connection.setDoOutput(true);

        // Data preparation for the request
        String requestBody = "code=" + authorizationCode +
                "&client_id=" + OAuthProperties.GOOGLE_CLIENT_ID +
                "&client_secret=" + OAuthProperties.GOOGLE_CLIENT_SECRET +
                "&redirect_uri=" + OAuthProperties.GOOGLE_REDIRECT_URI +
                "&grant_type=authorization_code";

        // Data transmission
        try (OutputStream os = connection.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            writer.write(requestBody);
            writer.flush();
        }

        // Response handling
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(response.toString());
                return jsonNode.get("access_token").asText();
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                return errorResponse.toString(); // Exception handling required...
            }
        }
    }

    public Map<String, Object> getGoogleUserInfo(String accessToken) throws IOException {
        String requestEndpoint = "https://www.googleapis.com/oauth2/v1/userinfo";
        URL url = new URL(requestEndpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> userInfoMap = objectMapper.readValue(response.toString(), Map.class);
                userInfoMap.put("provider", "google");
                return userInfoMap;
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(errorResponse.toString(), Map.class); // Exception handling required...
            }
        }
    }

    /**
     * 리소스 서버(kakao, google)로 부터 사용자 정보를 받은 후 그것을 바탕으로 회원가입
     * @param userInfoMap 사용자 정보 Map
     * @return UserDto (id, username)
     * @throws IOException
     */
    @Transactional
    public UserDto joinOAuthUser(Map<String, Object> userInfoMap) throws IOException {

        //OAuthUser 생성을 위한 매핑
        String provider = userInfoMap.get("provider").toString();
        String providerId = userInfoMap.get("id").toString();
        String username = provider + "_" + providerId;

        Map<String, Object> userInfo = new HashMap<>();

        userInfo.put("username", username);
        userInfo.put("provider", provider);
        userInfo.put("providerId", providerId);

        //OAuthUser 생성 -> 나중에 프로바이더마다 다른 회원가입 정책을 할 수도 있기 때문에 추상화
        OAuthUser oAuthUser = createOAuthUser(provider, userInfo);

        //db에 회원 등록이 되어있는지 확인후, 안되어 있다면 회원가입 시도
        User findUser = userDataRepository
                .findByUsername(oAuthUser.getName())
                .orElseGet(() -> {
                    User user = User.builder()
                            .provider(oAuthUser.getProvider())
                            .providerId(oAuthUser.getProviderId())
                            .username(oAuthUser.getName())
                            .role(UserRole.ROLE_USER)
                            .build();

                    // 회원가입
                    return userDataRepository.save(user);
                });
        return new UserDto(findUser.getId(), findUser.getUsername());

    }

    //OAuthUser 생성 메소드. 리소스 서버에 따라 분기.
    protected OAuthUser createOAuthUser(String provider, Map<String, Object> userInfo) {
        switch (provider) {
            case "google":
                return new GoogleUser(userInfo);
            case "kakao":
                return new KakaoUser(userInfo);
            default:
                return null;
        }
    }
}
