package com.tem.be.api.service;

public interface PasswordResetService {

    String requestPasswordReset(String email);

    boolean validateTemporaryPassword(String email, String tempPassword);

    void clearTemporaryPassword(String email);
}