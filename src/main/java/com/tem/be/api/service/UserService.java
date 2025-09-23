package com.tem.be.api.service;

import com.tem.be.api.dto.UserDTO;
import com.tem.be.api.model.User;

import java.util.List;

public interface UserService {
    User createUser(UserDTO userDTO);
    User updateUserById(Long id, UserDTO userDTO);

    void softDeleteUserById(Long id);

    List<User> getAllActiveUsers();

    User login(String email, String password);

    User getUserByEmail(String email);

    void changePasswordForgot(String email, String newPassword);

    String changePassword(String email, String oldPassword, String newPassword);

}
