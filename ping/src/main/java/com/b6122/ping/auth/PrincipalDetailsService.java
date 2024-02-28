package com.b6122.ping.auth;

import com.b6122.ping.domain.User;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// http://localhost:8080/login을 통해 로그인을 안함(disable 해놔서) 따라서 아래 서비스가 동작하기 위한
// 필터를 하나 만들어야 된다(JwtAuthenticationFilter)
// 근데 우리 서비스는 ~/login을 통해 요청할 일이 없기 때문에, JwtAuthenticationFilter도 사실상 필요가 없다
// jwt 토큰 발급 후 JwtAuthorizationFilter에서 시큐리티 세션에 Authentication 객체를 저장하여 권한 관리를 하고 있다.
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final UserDataRepository userDataRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userEntity = userDataRepository.findByUsername(username).orElseThrow();
        return new PrincipalDetails(userEntity);
    }
}
