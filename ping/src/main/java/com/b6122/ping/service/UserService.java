package com.b6122.ping.service;

import com.b6122.ping.domain.User;
import com.b6122.ping.dto.UpdateUserProfileDto;
import com.b6122.ping.dto.UserProfileReqDto;
import com.b6122.ping.dto.UserProfileResDto;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDataRepository userDataRepository;

    @Transactional
    public void updateProfile(UserProfileReqDto reqDto) {
        User user = userDataRepository.findById(reqDto.getId()).orElseThrow(RuntimeException::new);
        user.updateProfile(reqDto);
    }

    //계정 삭제
    @Transactional
    public void deleteAccount(Long id) {
        userDataRepository.deleteById(id);
    }

    /**
     * 사용자 정보(이미지, 닉네임) 가져오기
     * @param id 사용자의 id
     * @return 사용자 정보(UserProfileResDto 정보: nickname, profileImg, id)
     */
    public UserProfileResDto getUserProfile(Long id) {
        User user = userDataRepository.findById(id).orElseThrow(RuntimeException::new);
        return user.getProfileInfo();
    }

    /**
     * nickname으로 유저 검색
     * @param nickname
     * @return Long id of find user
     */
    public Long findUserByNickname(String nickname) {
        User findUser = userDataRepository.findByNickname(nickname).orElseThrow(EntityNotFoundException::new);
        return findUser.getId();
    }
}
