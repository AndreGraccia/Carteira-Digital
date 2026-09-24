package com.github.yildizmy.security;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.User;
import com.github.yildizmy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(mock(MessageSourceConfig.class), userRepository);
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void findsTheUserWhateverTheCaseAndSurroundingSpaces() {
        when(userRepository.findByUsernameIgnoreCase("Gui_Oliveira")).thenReturn(Optional.of(user()));

        assertEquals("gui_oliveira", service.loadUserByUsername(" Gui_Oliveira ").getUsername());
    }

    @Test
    void acceptsTheEmailAndKeepsTheStoredUsernameForTheToken() {
        when(userRepository.findByEmailIgnoreCase("Gui@Example.test")).thenReturn(Optional.of(user()));

        assertEquals("gui_oliveira", service.loadUserByUsername("Gui@Example.test").getUsername());
    }

    @Test
    void anEmailShapedUsernameDoesNotShadowTheOwnerOfThatEmail() {
        User squatter = user();
        squatter.setUsername("ana@x.io");
        when(userRepository.findByUsernameIgnoreCase("ana@x.io")).thenReturn(Optional.of(squatter));
        when(userRepository.findByEmailIgnoreCase("ana@x.io")).thenReturn(Optional.of(user()));

        assertEquals("gui_oliveira", service.loadUserByUsername("ana@x.io").getUsername());
    }

    @Test
    void unknownLoginIsRejected() {
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ninguem"));
    }

    private User user() {
        User user = new User();
        user.setId(8L);
        user.setUsername("gui_oliveira");
        user.setEmail("gui@example.test");
        user.setPassword("hash");
        user.setRoles(Set.of());
        return user;
    }
}
