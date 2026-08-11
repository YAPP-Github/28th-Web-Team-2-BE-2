package com.example.demo.auth.application.command;

import com.example.demo.auth.domain.ProviderType;

public record LoginCommand(ProviderType providerType, String idToken) {}
