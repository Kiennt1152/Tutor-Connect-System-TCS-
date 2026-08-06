package com.tcs.dev;

import com.tcs.module.identity.enums.UserStatus;
//import com.tcs.module.identity.enumst.OtpPurpose;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.identity.entity.User;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.Gender;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * DEV-ONLY seeder: tao 3 tai khoan onboarding test voi firstLogin=true (profile_completed_at = NULL).
 * Password chung: Test@1234
 *
 *   - onboarding.client@example.com   (CLIENT)
 *   - onboarding.tutor@example.com    (TUTOR)
 *   - onboarding.center@example.com   (TUTOR_CENTER)
 *
 * Chi kich hoat khi chay backend voi SPRING_PROFILES_ACTIVE=dev.
 */
@Configuration
@Profile("dev")
public class DevSeed {

    @Bean
    CommandLineRunner seedOnboardingAccounts(
            PasswordEncoder encoder,
            UserRepository userRepository,
            ClientRepository clientRepository,
            TutorRepository tutorRepository,
            TutorCenterRepository tutorCenterRepository) {
        return args -> {
            String rawPassword = "Test@1234";
            String hash = encoder.encode(rawPassword);

            seedClient(encoder, userRepository, clientRepository, hash,
                    "onboarding.client@example.com", "Client Onboarding", "0901234001");
            seedTutor(encoder, userRepository, tutorRepository, hash,
                    "onboarding.tutor@example.com", "Tutor Onboarding", "0901234002");
            seedCenter(encoder, userRepository, tutorCenterRepository, hash,
                    "onboarding.center@example.com", "Center Onboarding", "0901234003");

            System.out.println("=========================================================");
            System.out.println("  DEV SEED: 3 tai khoan onboarding test da san sang");
            System.out.println("  Password: Test@1234");
            System.out.println("  - onboarding.client@example.com");
            System.out.println("  - onboarding.tutor@example.com");
            System.out.println("  - onboarding.center@example.com");
            System.out.println("  firstLogin = true (profile_completed_at = NULL)");
            System.out.println("=========================================================");
        };
    }

    private void seedClient(PasswordEncoder enc, UserRepository users,
                            ClientRepository clients,
                            String hash, String email, String name, String phone) {
        User u = upsertUser(users, email, phone, hash);
        clients.findByUser_UserId(u.getUserId()).ifPresentOrElse(c -> {}, () -> {
            Client c = new Client();
            c.setUser(u);
            c.setFullName(name);
            c.setPhone(phone);
            clients.save(c);
        });
    }

    private void seedTutor(PasswordEncoder enc, UserRepository users,
                           TutorRepository tutors,
                           String hash, String email, String name, String phone) {
        User u = upsertUser(users, email, phone, hash);
        tutors.findByUser_UserId(u.getUserId()).ifPresentOrElse(t -> {}, () -> {
            Tutor t = new Tutor();
            t.setUser(u);
            t.setFullName(name);
            t.setGender(Gender.OTHER);
            t.setPhone(phone);
            tutors.save(t);
        });
    }

    private void seedCenter(PasswordEncoder enc, UserRepository users,
                            TutorCenterRepository centers,
                            String hash, String email, String name, String phone) {
        User u = upsertUser(users, email, phone, hash);
        centers.findByUser_UserId(u.getUserId()).ifPresentOrElse(c -> {}, () -> {
            TutorCenter c = new TutorCenter();
            c.setUser(u);
            c.setCompanyName(name);
            c.setLicenseNo("TEST-" + System.currentTimeMillis());
            c.setPhone(phone);
            c.setAddress("N/A");
            centers.save(c);
        });
    }

    /** Insert neu chua co; neu co roi reset profile_completed_at = NULL de dam bao firstLogin=true. */
    private User upsertUser(UserRepository repo, String email, String phone, String hash) {
        return repo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setPhone(phone);
            u.setPasswordHash(hash);
            u.setStatus(UserStatus.ACTIVE);
            u.setLastLogin(LocalDateTime.now());
            u.setProfileCompletedAt(null);  // Lan dau chua co.
            return repo.save(u);
        });
    }

}
