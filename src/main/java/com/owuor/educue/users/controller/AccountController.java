package com.owuor.educue.users.controller;

import com.owuor.educue.users.dto.ChangeMyPasswordRequest;
import com.owuor.educue.users.dto.MyProfileResponse;
import com.owuor.educue.users.dto.UpdateMyProfileRequest;
import com.owuor.educue.users.dto.AvatarUploadSignatureResponse;
import com.owuor.educue.users.dto.SaveAvatarRequest;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import com.owuor.educue.users.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountService accountService;
    @Value("${app.storage.cloudinary.cloud-name:}") private String cloudName;
    @Value("${app.storage.cloudinary.api-key:}") private String cloudinaryApiKey;
    @Value("${app.storage.cloudinary.api-secret:}") private String cloudinaryApiSecret;
    @Value("${app.default-avatar-url:https://ui-avatars.com/api/?name=User}") private String defaultAvatarUrl;

    @GetMapping("/profile")
    @PreAuthorize("!hasRole('STUDENT')")
    public MyProfileResponse profile(@AuthenticationPrincipal User user) { return response(user); }

    @PutMapping("/profile")
    @PreAuthorize("!hasRole('STUDENT')")
    public MyProfileResponse update(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {
        return accountService.updateProfile(
                authenticatedUser.getId(),
                request,
                defaultAvatarUrl
        );
    }

    @PostMapping("/avatar/signature")
    @PreAuthorize("!hasRole('STUDENT')")
    public AvatarUploadSignatureResponse avatarSignature(@AuthenticationPrincipal User user) {
        if (cloudName.isBlank() || cloudinaryApiKey.isBlank() || cloudinaryApiSecret.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary avatar uploads are not configured.");
        long timestamp = java.time.Instant.now().getEpochSecond();
        String folder = "profile-avatars";
        String publicId = "user-" + user.getId() + "-" + timestamp;
        String signature = sha1("folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp + cloudinaryApiSecret);
        return new AvatarUploadSignatureResponse("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload",
                cloudinaryApiKey, timestamp, folder, publicId, signature);
    }

    @PutMapping("/avatar")
    @PreAuthorize("!hasRole('STUDENT')")
    public MyProfileResponse saveAvatar(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody SaveAvatarRequest request
    ) {
        return accountService.saveAvatar(
                authenticatedUser.getId(),
                request.secureUrl(),
                cloudName,
                defaultAvatarUrl
        );
    }




    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal User authenticatedUser,
            @Valid @RequestBody ChangeMyPasswordRequest request
    ) {
        accountService.changePassword(
                authenticatedUser.getId(),
                request
        );
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String sha1(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("Could not sign avatar upload", e); }
    }
    private MyProfileResponse response(User user) {
        MyProfileResponse p = MyProfileResponse.from(user);
        return p.avatarUrl() == null ? new MyProfileResponse(p.id(), p.fullName(), p.email(), p.phone(), p.username(), defaultAvatarUrl, p.role(), p.departmentName()) : p;
    }
}
