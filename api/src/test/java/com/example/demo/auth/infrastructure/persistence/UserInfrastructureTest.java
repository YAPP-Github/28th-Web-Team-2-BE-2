package com.example.demo.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
class UserInfrastructureTest {

    private final UserJpaRepository userJpaRepository;

    @Autowired
    UserInfrastructureTest(final UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @BeforeEach
    void setUp() {
        userJpaRepository.deleteAll();
    }

    @Test
    void Kakao_사용자는_영속화된다() {
        final User saved = userJpaRepository.saveAndFlush(
                User.kakao("kakao-subject", "user@example.com", "Kakao User"));

        final User found = userJpaRepository.findById(saved.id()).orElseThrow();

        assertThat(found.providerSubject()).isEqualTo("kakao-subject");
        assertThat(found.email()).isEqualTo("user@example.com");
        assertThat(found.name()).isEqualTo("Kakao User");
        assertThat(found.role().name()).isEqualTo("USER");
    }

    @Test
    void 같은_provider와_subject는_중복_저장할_수_없다() {
        userJpaRepository.saveAndFlush(User.kakao("same-subject", null, "Kakao User"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(
                        User.kakao("same-subject", null, "Another User")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
