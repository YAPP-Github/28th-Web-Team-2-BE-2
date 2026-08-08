package com.example.demo.auth.application.port;

import com.example.demo.auth.domain.User;
import com.example.demo.auth.domain.ProviderType;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByProviderAndProviderSubject(ProviderType provider, String providerSubject);
}
