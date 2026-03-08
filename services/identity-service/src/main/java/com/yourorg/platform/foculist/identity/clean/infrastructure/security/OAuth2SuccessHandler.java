package com.yourorg.platform.foculist.identity.clean.infrastructure.security;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.security.oauth2.success-redirect-url:http://localhost:3000/login/success}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        if (!StringUtils.hasText(email)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not provided by provider");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setActive(true);
            newUser.setGlobalRole("USER");
            return userRepository.save(newUser);
        });

        String tenantId = TenantContext.require();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", StringUtils.hasText(user.getGlobalRole()) ? user.getGlobalRole() : "USER");
        claims.put("tenant", tenantId);
        String token = jwtService.generateAccessToken(user.getEmail(), claims);

        String targetUrl = UriComponentsBuilder.fromUriString(successRedirectUrl)
                .queryParam("token", token)
                .build(true)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
