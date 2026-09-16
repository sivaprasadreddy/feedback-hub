package dev.sivalabs.feedbackhub.users;

import dev.sivalabs.feedbackhub.shared.ResourceNotFoundException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountService {
    static final long MAX_PROFILE_PICTURE_SIZE = 1024 * 1024;
    private static final Set<String> SUPPORTED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final UserService userService;
    private final ProfilePictureRepository profilePictureRepository;

    AccountService(UserService userService, ProfilePictureRepository profilePictureRepository) {
        this.userService = userService;
        this.profilePictureRepository = profilePictureRepository;
    }

    @Transactional(readOnly = true)
    public AccountDetails getAccount(Long userId) {
        var user = userService.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AccountDetails(user.name(), user.email(), user.role(), profilePictureRepository.existsById(userId));
    }

    @Transactional
    public void saveProfilePicture(Long userId, String contentType, byte[] content) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Select an image to upload");
        }
        if (content.length > MAX_PROFILE_PICTURE_SIZE) {
            throw new IllegalArgumentException("Profile picture must not exceed 1 MB");
        }
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Use a JPEG, PNG, GIF, or WebP image");
        }
        if (userService.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        profilePictureRepository.save(new ProfilePictureEntity(userId, contentType, content));
    }

    @Transactional(readOnly = true)
    public ProfilePictureEntity getProfilePicture(Long userId) {
        return profilePictureRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile picture not found"));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordCmd cmd) {
        userService.changePassword(userId, cmd);
    }
}
