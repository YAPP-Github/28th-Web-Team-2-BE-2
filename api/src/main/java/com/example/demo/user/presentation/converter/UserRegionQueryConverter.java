package com.example.demo.user.presentation.converter;

import com.example.demo.common.security.AuthPrincipal;
import com.example.demo.user.application.query.GetUserRegionsQuery;
import org.springframework.stereotype.Component;

@Component
public class UserRegionQueryConverter {

    public GetUserRegionsQuery toQuery(final AuthPrincipal principal) {
        return new GetUserRegionsQuery(principal.userId());
    }
}
