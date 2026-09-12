package dev.sivalabs.speakup.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserStatisticsService {
    private final UserRepository userRepository;

    UserStatisticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }
}
