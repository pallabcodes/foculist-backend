package com.yourorg.platform.foculist.identity.clean.domain.port;

import java.util.Map;

public interface IdentityProviderPort {

    /**
     * Registers a new user with the Identity Provider.
     *
     * @param email The user's email address.
     * @param password The user's desired password.
     * @param attributes Additional user attributes (e.g., given_name, family_name, phone_number).
     * @return The unique provider ID assigned to the user (e.g., Cognito sub).
     */
    String registerUser(String email, String password, Map<String, String> attributes);

    /**
     * Authenticates a user and returns their session tokens.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A map containing token details (e.g., accessToken, idToken, refreshToken, expiresIn).
     */
    Map<String, String> authenticate(String email, String password);

    /**
     * Confirms an MFA challenge or email verification.
     *
     * @param email The relevant user's email address.
     * @param confirmationCode The TOTP/SMS code or email link token.
     */
    void confirmUser(String email, String confirmationCode);

    /**
     * Exchanges a valid refresh token for a new set of access tokens.
     *
     * @param refreshToken The active refresh token.
     * @return A map containing the new token details.
     */
    Map<String, String> refreshToken(String refreshToken);

    /**
     * Initiates a password reset flow.
     *
     * @param email The user's email address.
     */
    void forgotPassword(String email);

    /**
     * Completes a password reset flow using the confirmation code.
     *
     * @param email The user's email address.
     * @param confirmationCode The code sent to the user.
     * @param newPassword The new desired password.
     */
    void confirmForgotPassword(String email, String confirmationCode, String newPassword);

    /**
     * Changes the password of a logged-in user.
     *
     * @param email The user's email address.
     * @param accessToken The user's active access token.
     * @param oldPassword The user's current password.
     * @param newPassword The user's new desired password.
     */
    void changePassword(String email, String accessToken, String oldPassword, String newPassword);
}
