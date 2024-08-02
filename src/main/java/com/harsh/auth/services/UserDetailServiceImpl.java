package com.harsh.auth.services;

import com.harsh.auth.eventProducer.UserInfoEvent;
import com.harsh.auth.eventProducer.UserInfoProducer;
import com.harsh.auth.respositories.UserRepository;
import com.harsh.auth.entities.UserInfo;
import com.harsh.auth.model.UserInfoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoProducer userInfoProducer;

    @Autowired
    public UserDetailServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, UserInfoProducer userInfoProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userInfoProducer = userInfoProducer;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo user = userRepository.findByUsername(username);
        if(user == null) {
            throw new UsernameNotFoundException("Could not found the user!!");
        }

        return new CustomUserDetails(user);
    }
    public UserInfo checkIfUserExists(UserInfoDto userInfoDto) {
        return userRepository.findByUsername(userInfoDto.getUsername());
    }

    public Boolean signupUser(UserInfoDto userInfoDto) {
        // hashing the user's password
        userInfoDto.setPassword(passwordEncoder.encode(userInfoDto.getPassword()));
        if(checkIfUserExists(userInfoDto) != null){
            return Boolean.FALSE;
        }
        String userId = UUID.randomUUID().toString();
        userInfoDto.setUserId(userId);
        UserInfo user = new UserInfo
                (
                        userId,
                        userInfoDto.getUsername(),
                        userInfoDto.getPassword(),
                        new HashSet<>()
                );

        userRepository.save(user);

        //pushEventToMessagingService

        userInfoProducer.sendEventToKafka(userInfoToPublish(userInfoDto));

        return true;
    }
    private UserInfoEvent userInfoToPublish(UserInfoDto userInfoDto) {
        return UserInfoEvent.builder()
                .userId(userInfoDto.getUserId())
                .firstName(userInfoDto.getFirstName())
                .lastName(userInfoDto.getLastName())
                .email(userInfoDto.getEmail())
                .phoneNumber(userInfoDto.getPhoneNumber())
                .build();
    }
}
