package com.yourorg.platform.foculist.identity.clean.infrastructure.auth.aws;

import com.yourorg.platform.foculist.identity.clean.domain.port.IdentityProviderPort;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.Map;
import java.util.stream.Collectors;

public class AwsCognitoIdentityProviderAdapter implements IdentityProviderPort {

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;
    private final String clientId;

    public AwsCognitoIdentityProviderAdapter(CognitoIdentityProviderClient cognitoClient, String userPoolId, String clientId) {
        this.cognitoClient = cognitoClient;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
    }

    @Override
    public String registerUser(String email, String password, Map<String, String> attributes) {
        var userAttributes = attributes.entrySet().stream()
                .map(e -> AttributeType.builder().name(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
        
        userAttributes.add(AttributeType.builder().name("email").value(email).build());

        var request = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(userAttributes)
                .build();

        SignUpResponse response = cognitoClient.signUp(request);
        return response.userSub();
    }

    @Override
    public Map<String, String> authenticate(String email, String password) {
        var authParams = Map.of(
                "USERNAME", email,
                "PASSWORD", password
        );

        var request = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        InitiateAuthResponse response = cognitoClient.initiateAuth(request);
        AuthenticationResultType authResult = response.authenticationResult();

        return Map.of(
                "accessToken", authResult.accessToken(),
                "idToken", authResult.idToken(),
                "refreshToken", authResult.refreshToken(),
                "expiresIn", String.valueOf(authResult.expiresIn())
        );
    }

    @Override
    public void confirmUser(String email, String confirmationCode) {
        var request = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(confirmationCode)
                .build();
        cognitoClient.confirmSignUp(request);
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        var authParams = Map.of(
                "REFRESH_TOKEN", refreshToken
        );

        var request = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .clientId(clientId)
                .authParameters(authParams)
                .build();

        InitiateAuthResponse response = cognitoClient.initiateAuth(request);
        AuthenticationResultType authResult = response.authenticationResult();

        return Map.of(
                "accessToken", authResult.accessToken(),
                "idToken", authResult.idToken(),
                "expiresIn", String.valueOf(authResult.expiresIn())
        );
    }

    @Override
    public void forgotPassword(String email) {
        var request = ForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email)
                .build();
        cognitoClient.forgotPassword(request);
    }

    @Override
    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        var request = ConfirmForgotPasswordRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(confirmationCode)
                .password(newPassword)
                .build();
        cognitoClient.confirmForgotPassword(request);
    }
}
