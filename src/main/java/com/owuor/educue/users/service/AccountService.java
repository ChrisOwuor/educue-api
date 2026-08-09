package com.owuor.educue.users.service;

import com.owuor.educue.users.dto.ChangeMyPasswordRequest;
import com.owuor.educue.users.dto.MyProfileResponse;
import com.owuor.educue.users.dto.UpdateMyProfileRequest;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional(readOnly = true)
    public MyProfileResponse getProfile(
            Long userId,
            String defaultAvatarUrl
    ) {
        User user = findProfileUser(userId);

        return response(
                user,
                defaultAvatarUrl
        );
    }

    @Transactional
    public MyProfileResponse saveAvatar(
            Long userId,
            String secureUrl,
            String cloudName,
            String defaultAvatarUrl
    ) {
        /*
         * Correct argument order:
         * secure URL first, cloud name second.
         */
        validateCloudinaryUrl(
                secureUrl,
                cloudName
        );

        User user = findProfileUser(userId);

        user.setAvatarUrl(secureUrl);

        /*
         * Explicit save is optional because user is managed
         * within this transaction.
         */
        userRepository.save(user);

        /*
         * Map the response while role and department are still
         * attached to the Hibernate session.
         */
        return response(
                user,
                defaultAvatarUrl
        );
    }

    private User findProfileUser(Long userId) {
        return userRepository
                .findProfileById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User profile not found."
                        )
                );
    }

    @Transactional
    public MyProfileResponse updateProfile(
            Long userId,
            UpdateMyProfileRequest request,
            String defaultAvatarUrl
    ) {
        User user = userRepository
                .findProfileById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User profile not found."
                        )
                );

        String username = clean(
                request.username()
        );

        if (username != null
            && userRepository
                    .existsByUsernameIgnoreCaseAndIdNot(
                            username,
                            userId
                    )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "That username is already in use."
            );
        }

        user.setFullName(
                request.fullName().trim()
        );

        user.setUsername(username);

        /*
         * Explicit save is optional because user is managed.
         * Hibernate dirty checking updates it when transaction commits.
         */
        userRepository.save(user);

        return response(
                user,
                defaultAvatarUrl
        );
    }

    @Transactional
    public void changePassword(
            Long userId,
            ChangeMyPasswordRequest request
    ) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User account not found."
                        )
                );

        String currentPassword =
                request.currentPassword();

        String newPassword =
                request.newPassword();

        if (!passwordEncoder.matches(
                currentPassword,
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect."
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must be different."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        user.setMustChangePassword(false);

        /*
         * Explicit save is unnecessary because the User is managed
         * inside this transaction. Hibernate dirty checking will persist
         * the changes when the transaction commits.
         */
    }

    private MyProfileResponse response(
            User user,
            String defaultAvatarUrl
    ) {
        MyProfileResponse profile =
                MyProfileResponse.from(user);

        if (profile.avatarUrl() != null
            && !profile.avatarUrl().isBlank()) {
            return profile;
        }

        return new MyProfileResponse(
                profile.id(),
                profile.fullName(),
                profile.email(),
                profile.phone(),
                profile.username(),
                defaultAvatarUrl,
                profile.role(),
                profile.departmentName()
        );
    }

    private void validateCloudinaryUrl(
            String secureUrl,
            String cloudName
    ) {
        if (cloudName == null
            || cloudName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloudinary is not configured."
            );
        }

        if (secureUrl == null
            || secureUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Avatar URL is required."
            );
        }

        String expectedPrefix =
                "https://res.cloudinary.com/"
                + cloudName
                + "/";

        if (!secureUrl.startsWith(expectedPrefix)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Cloudinary avatar URL."
            );
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }
}
