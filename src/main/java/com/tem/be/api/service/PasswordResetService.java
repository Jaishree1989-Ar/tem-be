package com.tem.be.api.service;

public interface PasswordResetService {
    void requestPasswordReset(String email);
    void resetPassword(String email, String tempPassword, String newPassword);
    boolean validateTemporaryPassword(String email, String tempPassword);
    void clearTemporaryPassword(String email);
}