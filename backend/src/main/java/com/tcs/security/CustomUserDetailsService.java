package com.tcs.security;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        UserProfileBundle profiles = UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(user.getUserId()).orElse(null),
                tutorRepository.findByUser_UserId(user.getUserId()).orElse(null),
                tutorCenterRepository.findByUser_UserId(user.getUserId()).orElse(null),
                clientRepository.findByUser_UserId(user.getUserId()).orElse(null));
        return new UserPrincipal(user, platformMapper.resolveRole(profiles));
    }
}
