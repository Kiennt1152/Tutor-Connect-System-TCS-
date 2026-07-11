package com.tcs.config;

import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ContractRepository contractRepository;

    @Override
    public void run(String... args) {
        seedPlatformAdmin("thanhkiu0209@gmail.com", "12345678", "Dev Admin");

        // Seed test accounts
        String[][] testAccounts = {
            {"test.client67@tcs.com", "password123", "Test Client"},
            {"test.tutor67@tcs.com", "password123", "Test Tutor"},
            {"test.center67@tcs.com", "password123", "Test Center"}
        };

        for (String[] acc : testAccounts) {
            User user = userRepository.findByEmail(acc[0]).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(acc[0]);
                user.setPasswordHash(passwordEncoder.encode(acc[1]));
                user.setStatus(UserStatus.ACTIVE);
                user = userRepository.save(user);
                System.out.println(">>> [DevDataSeeder] Da tao test account: " + acc[0] + " / " + acc[1]);
            } else {
                // Update password for existing test users
                user.setPasswordHash(passwordEncoder.encode(acc[1]));
                userRepository.save(user);
                System.out.println(">>> [DevDataSeeder] Da cap nhat password cho: " + acc[0] + " / " + acc[1]);
            }
        }

        // Seed contract cho test tutor
        seedContractForTutor("test.tutor67@tcs.com");
    }

    private void seedContractForTutor(String tutorEmail) {
        User tutorUser = userRepository.findByEmail(tutorEmail).orElse(null);
        if (tutorUser == null) {
            System.out.println(">>> [DevDataSeeder] Khong tim thay user: " + tutorEmail);
            return;
        }

        Tutor tutor = tutorRepository.findByUser_UserId(tutorUser.getUserId()).orElse(null);
        if (tutor == null) {
            System.out.println(">>> [DevDataSeeder] Khong tim thay tutor profile cho: " + tutorEmail);
            return;
        }

        // Kiem tra xem da co contract cho tutor chua
        boolean hasContract = !contractRepository.findByAssignment_Tutor_UserId(tutorUser.getUserId()).isEmpty();
        if (hasContract) {
            System.out.println(">>> [DevDataSeeder] Tutor " + tutorEmail + " da co contract, skip.");
            return;
        }

        // Tao tutoring class
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(tutorUser);
        tutoringClass.setClassType(ClassType.PRIVATE);
        tutoringClass.setLessonMode(LessonMode.OFFLINE);
        tutoringClass.setRecurringType(RecurringType.ONCE);
        tutoringClass.setStatus(TutoringClassStatus.IN_PROGRESS);
        tutoringClass.setTitle("Lop hoc test - " + tutor.getFullName());
        tutoringClass.setDescription("Lop hoc duoc tao tu DevDataSeeder de test");
        tutoringClass.setNumberOfSessions(10);
        tutoringClass.setStartDate(LocalDate.now());
        tutoringClass.setEndDate(LocalDate.now().plusMonths(3));
        tutoringClass.setTuitionFee(new BigDecimal("1500000"));
        tutoringClass = tutoringClassRepository.save(tutoringClass);
        System.out.println(">>> [DevDataSeeder] Da tao tutoring class: " + tutoringClass.getClassId());

        // Tao assignment
        ClassAssignment assignment = new ClassAssignment();
        assignment.setTutor(tutor);
        assignment.setStatus(ClassAssignmentStatus.ACTIVE);
        assignment = classAssignmentRepository.save(assignment);
        System.out.println(">>> [DevDataSeeder] Da tao assignment: " + assignment.getAssignmentId());

        // Tao contract
        Contract contract = new Contract();
        contract.setAssignment(assignment);
        contract.setStatus(com.tcs.module.contract.enums.ContractStatus.DRAFT);
        contract.setTermsSummary("Hop dong dich vu gia su tai nha. Hoc phi: 150,000 VND/giang.");
        contract.setContractNo("TCS-TEST-" + System.currentTimeMillis());
        contract = contractRepository.save(contract);
        System.out.println(">>> [DevDataSeeder] Da tao contract: " + contract.getContractId() + " cho tutor: " + tutorEmail);
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
