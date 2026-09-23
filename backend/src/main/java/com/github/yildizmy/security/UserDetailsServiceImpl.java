package com.github.yildizmy.security;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.github.yildizmy.common.MessageKeys.ERROR_USERNAME_NOT_FOUND;

/**
 * Service used for UserDetails related operations.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MessageSourceConfig messageConfig;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Sign-up rejects names and e-mails that differ only by case, so each lookup finds at most one user.
        // Accepting either field, in any case, stops "Gui_oliveira" or the e-mail from being rejected at login.
        final String login = username == null ? "" : username.trim();
        final User user = userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .orElseThrow(() -> new UsernameNotFoundException(messageConfig.getMessage(ERROR_USERNAME_NOT_FOUND, username)));
        return UserDetailsImpl.build(user);
    }
}
