package com.juahaki.juahaki.mapper;

import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.model.user.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserInfo mapToUserInfo(User user) {
        if (user == null) {
            return null;
        }
        return modelMapper.map(user, UserInfo.class);
    }

    public User mapToUser(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }
        return modelMapper.map(userInfo, User.class);
    }

    public void updateUserFromUserInfo(UserInfo userInfo, User user) {
        if (userInfo == null || user == null) {
            return;
        }
        modelMapper.map(userInfo, user);
    }
}
