package com.tcs.config;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tạo sẵn 1 tài khoản test khi khởi động (chỉ phục vụ dev — có thể xóa file này).
 * Đăng nhập bằng: test@tcs.com / 123456
 */
@Component
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = "test@tcs.com";
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("123456"));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            System.out.println(">>> [DevDataSeeder] Đã tạo tài khoản test: " + email + " / 123456");
        }
    }
}
