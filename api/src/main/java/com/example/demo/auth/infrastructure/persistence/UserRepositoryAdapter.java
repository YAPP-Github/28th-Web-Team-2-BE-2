package com.example.demo.auth.infrastructure.persistence;

import com.example.demo.auth.application.port.UserRepository;
import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.UserProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(final User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(final Long userId) {
        return userJpaRepository.findById(userId);
    }

    @Override
    public Optional<User> findByProviderAndProviderSubject(
            final UserProvider provider, final String providerSubject) {
        return userJpaRepository.findByProviderAndProviderSubject(provider, providerSubject);
    }
}
