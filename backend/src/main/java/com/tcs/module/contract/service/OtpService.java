package com.tcs.module.contract.service;

import com.tcs.module.contract.entity.ContractOtp;
import com.tcs.module.contract.repository.ContractOtpRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.service.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final ContractOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    private static final int OTP_VALID_MINUTES = 5;

    @Transactional
    public String generateAndSendOtp(Long contractId, Long signerUserId, String contractNo) {
        User signer = userRepository.findById(signerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));

        otpRepository.deleteByContractIdAndSignerUserId(contractId, signerUserId);

        String otpCode = generateOtp();
        ContractOtp record = new ContractOtp();
        record.setContractId(contractId);
        record.setSignerUserId(signerUserId);
        record.setOtpCode(otpCode);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
        otpRepository.save(record);

        emailService.sendContractOtp(signer.getEmail(), otpCode, contractNo, OTP_VALID_MINUTES);

        return maskEmail(signer.getEmail());
    }

    @Transactional
    public boolean verifyOtp(Long contractId, Long signerUserId, String rawOtp) {
        var optRecord = otpRepository
                .findFirstByContractIdAndSignerUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(contractId, signerUserId);

        if (optRecord.isEmpty()) {
            return false;
        }

        ContractOtp record = optRecord.get();

        if (record.isExpired()) {
            return false;
        }

        if (!record.getOtpCode().equals(rawOtp)) {
            return false;
        }

        record.setConsumedAt(LocalDateTime.now());
        otpRepository.save(record);
        return true;
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, 5);
        return String.format("%06d", random.nextInt(9 * bound) + bound);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(at);
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
