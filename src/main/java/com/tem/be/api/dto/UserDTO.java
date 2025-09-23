package com.tem.be.api.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) representing user information.
 * Used to transfer user related data between layers of the application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String userName;
    private String email;
    private String phoneNumber;
    private long roleId;
    private long cityId;
    private long deptId;
}
