package com.example.demo.auth.infrastructure.persistence;

import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.ProviderType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByNickname(String nickname);

    Optional<User> findByProviderAndProviderSubject(ProviderType provider, String providerSubject);
}
