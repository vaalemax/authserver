package com.portfolio.authserver.user.presentation.mapper;

import com.portfolio.authserver.user.presentation.dto.UserResponse;
import com.portfolio.authserver.user.domain.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles(), user.isEnabled());
    }
}
