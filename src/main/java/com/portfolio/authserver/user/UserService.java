package com.portfolio.authserver.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    public UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRoles(), user.isEnabled());
    }
}
