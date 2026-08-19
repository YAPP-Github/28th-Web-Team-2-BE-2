package com.example.demo.user.presentation.command;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.application.command.AddUserRegionCommand;
import com.example.demo.user.application.command.SetCurrentUserRegionCommand;
import com.example.demo.user.presentation.dto.AddUserRegionRequest;
import org.springframework.stereotype.Component;

@Component
public class UserRegionCommandConverter {

    public AddUserRegionCommand toAddCommand(
            final AddUserRegionRequest request, final AuthPrincipal principal) {
        return new AddUserRegionCommand(principal.userId(), request.regionId());
    }

    public SetCurrentUserRegionCommand toSetCurrentCommand(
            final String regionId, final AuthPrincipal principal) {
        return new SetCurrentUserRegionCommand(principal.userId(), regionId);
    }
}
