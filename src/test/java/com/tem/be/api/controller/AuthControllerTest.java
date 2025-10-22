package com.tem.be.api.controller;

import com.tem.be.api.dto.LoginRequest;
import com.tem.be.api.model.User;
import com.tem.be.api.service.UserService;
import com.tem.be.api.utils.RestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_ShouldReturnSuccessResponse() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        User mockUser = new User();
        mockUser.setUserId(10L);
        mockUser.setUserName("JackDoe");
        mockUser.setEmail("jack@example.com");

        when(userService.login(request.getEmail(), request.getPassword())).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(RestConstants.SUCCESS_CODE))
                .andExpect(jsonPath("$.status").value(RestConstants.SUCCESS_STRING))
                .andExpect(jsonPath("$.data.email").value("jack@example.com"))
                .andExpect(jsonPath("$.data.userName").value("JackDoe"));
    }

    @Test
    void login_ShouldReturnInternalServerError_WhenLoginFails() throws Exception {
        LoginRequest request = new LoginRequest("jack@example.com", "12345678");

        when(userService.login(request.getEmail(), request.getPassword()))
                .thenThrow(new RuntimeException("Invalid password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Invalid password"));
    }

    @Test
    void login_ShouldReturnInternalServerError_WhenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest("jack@example.com", "12345678");

        when(userService.login(request.getEmail(), request.getPassword()))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("User not found"));
    }

}