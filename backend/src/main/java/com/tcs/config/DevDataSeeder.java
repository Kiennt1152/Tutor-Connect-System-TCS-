package com.tcs.config;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedPlatformAdmin("thanhkiu0209@gmail.com", "12345678", "Dev Admin");
    }

    private void seedPlatformAdmin(String email, String rawPassword, String fullName) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
            System.out.println(">>> [DevDataSeeder] Da tao tai khoan: " + email + " / " + rawPassword);
        }

        if (platformAdminRepository.findByUser_UserId(user.getUserId()).isEmpty()) {
            PlatformAdmin admin = new PlatformAdmin();
            admin.setUser(user);
            admin.setFullName(fullName);
            platformAdminRepository.save(admin);
            System.out.println(">>> [DevDataSeeder] Da gan profile PLATFORM_ADMIN cho: " + email);
        }
    }
}
