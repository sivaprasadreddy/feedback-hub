package dev.sivalabs.speakup.users;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UsersAPI {
    private final UserService userService;

    UsersAPI(UserService userService) {
        this.userService = userService;
    }

    public Optional<String> findNameById(Long userId) {
        return userService.findById(userId).map(UserDto::name);
    }
}
