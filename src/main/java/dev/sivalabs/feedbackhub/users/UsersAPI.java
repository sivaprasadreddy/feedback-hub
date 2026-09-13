package dev.sivalabs.feedbackhub.users;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    public Map<Long, String> findNamesByIds(Set<Long> userIds) {
        return userService.findByIds(userIds).stream()
                .collect(Collectors.toMap(UserDto::id, UserDto::name, (first, second) -> first));
    }
}
