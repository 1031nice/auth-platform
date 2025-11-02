package com.auth.server.security;

import com.auth.server.domain.entity.Role;
import com.auth.server.domain.entity.User;
import com.auth.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsService tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(List.of(Role.ROLE_USER))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should load UserDetails by username for existing user")
    void shouldLoadUserDetailsByUsername() {
        // given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo(testUser.getPassword());
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: " + username);
    }

    @Test
    @DisplayName("Should load UserDetails with multiple roles")
    void shouldLoadUserDetailsWithMultipleRoles() {
        // given
        User multiRoleUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@example.com")
                .password("encodedPassword")
                .roles(List.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(multiRoleUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("adminuser");

        // then
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList())
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should load UserDetails for disabled user")
    void shouldLoadUserDetailsForDisabledUser() {
        // given
        User disabledUser = User.builder()
                .id(3L)
                .username("disableduser")
                .email("disabled@example.com")
                .password("encodedPassword")
                .roles(List.of(Role.ROLE_USER))
                .enabled(false)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(disabledUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

        // then
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should load UserDetails for expired account")
    void shouldLoadUserDetailsForExpiredAccount() {
        // given
        User expiredUser = User.builder()
                .id(4L)
                .username("expireduser")
                .email("expired@example.com")
                .password("encodedPassword")
                .roles(List.of(Role.ROLE_USER))
                .enabled(true)
                .accountNonExpired(false)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(userRepository.findByUsername("expireduser")).thenReturn(Optional.of(expiredUser));

        // when
        UserDetails userDetails = userDetailsService.loadUserByUsername("expireduser");

        // then
        assertThat(userDetails.isAccountNonExpired()).isFalse();
    }
}
