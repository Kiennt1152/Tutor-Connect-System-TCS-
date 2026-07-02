package com.tcs.module.identity.service.impl;

import com.tcs.exception.DuplicateEmailException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.entity.Wallet;
import com.tcs.module.finance.repository.WalletRepository;
import com.tcs.module.identity.dto.request.ChangePasswordRequest;
import com.tcs.module.identity.dto.request.ForgotPasswordRequest;
import com.tcs.module.identity.dto.request.LoginRequest;
import com.tcs.module.identity.dto.request.RegisterRequest;
import com.tcs.module.identity.dto.request.ResetPasswordRequest;
import com.tcs.module.identity.dto.response.AuthResponse;
import com.tcs.module.identity.dto.response.MeResponse;
import com.tcs.module.identity.entity.PasswordResetToken;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.PasswordResetTokenRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.IdentityService;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import com.tcs.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IdentityServiceImpl implements IdentityService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final WalletRepository walletRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlatformMapper platformMapper;
    private final AuthHelper authHelper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getStatus() == UserStatus.BANNED) {
                throw new DuplicateEmailException(
                        "Email này đã bị khóa vĩnh viễn và không thể đăng ký tài khoản mới");
            }
            throw new DuplicateEmailException("Email đã được sử dụng");
        });
        if (request.getRole() == UserRole.PLATFORM_ADMIN || request.getRole() == UserRole.UNKNOWN) {
            throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        UserProfileBundle profiles = createProfile(savedUser, request);
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        walletRepository.save(wallet);

        UserRole role = platformMapper.resolveRole(profiles);
        String token = jwtService.generateToken(savedUser.getUserId(), savedUser.getEmail(), role);
        return buildAuthResponse(savedUser, profiles, token);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa và không thể đăng nhập");
        }
        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalArgumentException("Tài khoản không thể đăng nhập");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        UserProfileBundle profiles = loadProfiles(user.getUserId());
        UserRole role = platformMapper.resolveRole(profiles);
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), role);
        return buildAuthResponse(user, profiles, token);
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe() {
        Long userId = authHelper.currentUserId();
        User user = findUser(userId);
        UserProfileBundle profiles = loadProfiles(userId);
        return MeResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(platformMapper.resolveRole(profiles))
                .status(user.getStatus())
                .displayName(platformMapper.toUserListItem(user, profiles).getDisplayName())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = findUser(authHelper.currentUserId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.BANNED) {
                return;
            }
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(LocalDateTime.now().plusHours(24));
            passwordResetTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đã hết hạn hoặc đã sử dụng");
        }
        User user = token.getUser();
        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa và không thể đặt lại mật khẩu");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    private UserProfileBundle createProfile(User user, RegisterRequest request) {
        return switch (request.getRole()) {
            case CLIENT -> {
                Client client = new Client();
                client.setUser(user);
                client.setFullName(request.getFullName());
                client.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : "0000000000");
                clientRepository.save(client);
                yield UserProfileBundle.of(null, null, null, client);
            }
            case TUTOR -> {
                Tutor tutor = new Tutor();
                tutor.setUser(user);
                tutor.setFullName(request.getFullName());
                tutor.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : "0000000000");
                tutor.setGender(request.getGender() != null ? request.getGender() : Gender.OTHER);
                tutor.setExperienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0);
                tutor.setHourlyRate(request.getHourlyRate() != null ? request.getHourlyRate() : BigDecimal.ZERO);
                tutor.setAddress(request.getAddress());
                tutorRepository.save(tutor);
                yield UserProfileBundle.of(null, tutor, null, null);
            }
            case TUTOR_CENTER -> {
                if (!StringUtils.hasText(request.getCompanyName()) || !StringUtils.hasText(request.getLicenseNo())) {
                    throw new IllegalArgumentException("Tên trung tâm và mã giấy phép là bắt buộc");
                }
                TutorCenter center = new TutorCenter();
                center.setUser(user);
                center.setCompanyName(request.getCompanyName());
                center.setLicenseNo(request.getLicenseNo());
                center.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : "0000000000");
                center.setAddress(StringUtils.hasText(request.getAddress()) ? request.getAddress() : "N/A");
                tutorCenterRepository.save(center);
                yield UserProfileBundle.of(null, null, center, null);
            }
            default -> throw new IllegalArgumentException("Vai trò đăng ký không hợp lệ");
        };
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private AuthResponse buildAuthResponse(User user, UserProfileBundle profiles, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(platformMapper.resolveRole(profiles))
                .displayName(platformMapper.toUserListItem(user, profiles).getDisplayName())
                .status(user.getStatus())
                .build();
    }
}
