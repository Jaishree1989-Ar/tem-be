package com.tem.be.api.service;

import com.tem.be.api.dao.AccountDepartmentMappingDao;
import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.exception.DuplicateResourceException;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.AccountDepartmentMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;

/**
 * Unit tests for the {@link AccountDepartmentMappingServiceImpl} class.
 * <p>
 * This class isolates the service layer logic by mocking the {@link AccountDepartmentMappingDao} dependency.
 * Each test verifies a specific business rule, such as data retrieval, creation, validation,
 * updates, deletion, and file processing logic.
 */
@ExtendWith(MockitoExtension.class)
class AccountDepartmentMappingServiceImplTest {

    /**
     * Mocked DAO layer to simulate database interactions without a real database connection.
     */
    @Mock
    private AccountDepartmentMappingDao mappingDao;

    /**
     * The instance of the service being tested, with mocked dependencies injected.
     */
    @InjectMocks
    private AccountDepartmentMappingServiceImpl mappingService;

    // Reusable test data objects
    private AccountDepartmentMapping mapping1;
    private AccountDepartmentMappingDTO mappingDTO;

    /**
     * Sets up common test data before each test execution. This ensures a consistent
     * starting state for each test case.
     */
    @BeforeEach
    void setUp() {
        mapping1 = new AccountDepartmentMapping();
        mapping1.setId(1L);
        mapping1.setCarrier("FirstNet");
        mapping1.setDepartment("Sales");
        mapping1.setDepartmentAccountNumber("ACC123");

        mappingDTO = new AccountDepartmentMappingDTO();
        mappingDTO.setCarrier("FirstNet");
        mappingDTO.setDepartment("Engineering");
        mappingDTO.setDepartmentAccountNumber("ACC789");
    }

    /**
     * Tests that {@code getMappingById} successfully returns a mapping when a valid ID is provided.
     */
    @Test
    @DisplayName("getMappingById - Should return mapping when found")
    void getMappingById_whenFound_shouldReturnMapping() {
        // Arrange
        when(mappingDao.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(mapping1));

        // Act
        AccountDepartmentMapping foundMapping = mappingService.getMappingById(1L);

        // Assert
        assertThat(foundMapping).isNotNull();
        assertThat(foundMapping.getId()).isEqualTo(1L);
        verify(mappingDao).findByIdAndIsDeletedFalse(1L);
    }

    /**
     * Tests that {@code getMappingById} throws a {@link ResourceNotFoundException} when the mapping ID does not exist.
     */
    @Test
    @DisplayName("getMappingById - Should throw ResourceNotFoundException when not found")
    void getMappingById_whenNotFound_shouldThrowException() {
        // Arrange
        long nonExistentId = 99L;
        when(mappingDao.findByIdAndIsDeletedFalse(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                mappingService.getMappingById(nonExistentId)
        );

        assertEquals("Mapping not found with ID: " + nonExistentId, exception.getMessage());
    }

    /**
     * Tests that a new mapping is successfully created and saved when the account number is not a duplicate.
     */
    @Test
    @DisplayName("createMapping - Should create and return new mapping")
    void createMapping_whenNotDuplicate_shouldCreateMapping() {
        // Arrange
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(mappingDTO.getDepartmentAccountNumber()))
                .thenReturn(false);
        when(mappingDao.save(any(AccountDepartmentMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountDepartmentMapping createdMapping = mappingService.createMapping(mappingDTO);

        // Assert
        assertThat(createdMapping).isNotNull();
        assertThat(createdMapping.getDepartment()).isEqualTo("Engineering");
        assertThat(createdMapping.getDepartmentAccountNumber()).isEqualTo("ACC789");
        verify(mappingDao).save(any(AccountDepartmentMapping.class));
    }

    /**
     * Tests that {@code createMapping} throws a {@link DuplicateResourceException} if a mapping
     * with the same account number already exists.
     */
    @Test
    @DisplayName("createMapping - Should throw DuplicateResourceException when mapping already exists")
    void createMapping_whenDuplicate_shouldThrowException() {
        // Arrange
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse(mappingDTO.getDepartmentAccountNumber()))
                .thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () ->
                mappingService.createMapping(mappingDTO)
        );
        verify(mappingDao, never()).save(any());
    }

    /**
     * Tests that an existing mapping is successfully updated with new information.
     */
    @Test
    @DisplayName("updateMapping - Should update and return mapping")
    void updateMapping_shouldUpdateAndReturnMapping() {
        // Arrange
        Long mappingId = 1L;
        AccountDepartmentMapping existingMapping = new AccountDepartmentMapping();
        existingMapping.setId(mappingId);
        existingMapping.setDepartmentAccountNumber("OLD_ACC");

        AccountDepartmentMappingDTO updateDTO = new AccountDepartmentMappingDTO();
        updateDTO.setDepartment("Updated Dept");
        updateDTO.setDepartmentAccountNumber("NEW_ACC");

        when(mappingDao.findByIdAndIsDeletedFalse(mappingId)).thenReturn(Optional.of(existingMapping));
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse("NEW_ACC")).thenReturn(false);
        when(mappingDao.save(any(AccountDepartmentMapping.class))).thenReturn(existingMapping);

        // Act
        AccountDepartmentMapping updatedMapping = mappingService.updateMapping(mappingId, updateDTO);

        // Assert
        assertThat(updatedMapping).isNotNull();
        assertThat(updatedMapping.getDepartment()).isEqualTo("Updated Dept");
        verify(mappingDao).save(existingMapping);
    }

    /**
     * Tests that {@code updateMapping} throws a {@link DuplicateResourceException} if the update attempts
     * to use an account number that already belongs to another mapping.
     */
    @Test
    @DisplayName("updateMapping - Should throw DuplicateResourceException on update")
    void updateMapping_whenDuplicate_shouldThrowException() {
        // Arrange
        Long mappingId = 1L;
        AccountDepartmentMapping existingMapping = new AccountDepartmentMapping();
        existingMapping.setId(mappingId);
        existingMapping.setDepartmentAccountNumber("OLD_ACC");

        AccountDepartmentMappingDTO updateDTO = new AccountDepartmentMappingDTO();
        updateDTO.setDepartmentAccountNumber("DUPLICATE_ACC");

        when(mappingDao.findByIdAndIsDeletedFalse(mappingId)).thenReturn(Optional.of(existingMapping));
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse("DUPLICATE_ACC")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateResourceException.class, () ->
                mappingService.updateMapping(mappingId, updateDTO)
        );
        verify(mappingDao, never()).save(any());
    }

    /**
     * Tests that {@code deleteMapping} correctly calls the DAO's delete method when the mapping exists.
     */
    @Test
    @DisplayName("deleteMapping - Should call deleteById when mapping exists")
    void deleteMapping_whenExists_shouldCallDelete() {
        // Arrange
        Long mappingId = 1L;
        when(mappingDao.existsById(mappingId)).thenReturn(true);
        doNothing().when(mappingDao).deleteById(mappingId); // Stubbing for void method

        // Act
        mappingService.deleteMapping(mappingId);

        // Assert
        verify(mappingDao, times(1)).deleteById(mappingId);
    }

    /**
     * Tests that {@code deleteMapping} throws a {@link ResourceNotFoundException} when attempting
     * to delete a mapping that does not exist.
     */
    @Test
    @DisplayName("deleteMapping - Should throw ResourceNotFoundException when mapping does not exist")
    void deleteMapping_whenNotExists_shouldThrowException() {
        // Arrange
        Long nonExistentId = 99L;
        when(mappingDao.existsById(nonExistentId)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                mappingService.deleteMapping(nonExistentId)
        );
        verify(mappingDao, never()).deleteById(anyLong());
    }

    /**
     * Tests the CSV upload functionality, ensuring that it correctly parses the file,
     * filters out duplicate records, and saves only the new mappings.
     *
     * @throws IOException if file processing fails.
     */
    @Test
    @DisplayName("uploadMappings - Should process CSV and save new mappings")
    void uploadMappings_withNewData_shouldSaveAndReturn() throws IOException {
        // Arrange
        String content = "FAN,ACCOUNT #,DEPT\nFAN1,NEW_ACC_1,Dept1\nFAN2,ACC123,Dept2"; // ACC123 is a duplicate
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", content.getBytes());

        // Mock DB checks: ACC123 exists, NEW_ACC_1 does not.
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse("NEW_ACC_1")).thenReturn(false);
        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse("ACC123")).thenReturn(true);
        when(mappingDao.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<AccountDepartmentMapping> result = mappingService.uploadMappings(file, "testUser", "firstnet");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartmentAccountNumber()).isEqualTo("NEW_ACC_1");
        verify(mappingDao).saveAll(anyList());
    }

    /**
     * Tests that the upload process throws an {@link IllegalArgumentException} if the uploaded file has no name.
     */
    @Test
    @DisplayName("uploadMappings - Should throw exception for empty filename")
    void uploadMappings_withEmptyFilename_shouldThrowException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "", "text/csv", "content".getBytes());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                mappingService.uploadMappings(file, "user", "firstnet")
        );
        assertEquals("File name cannot be empty.", ex.getMessage());
    }

    /**
     * Tests that the upload process throws an {@link IllegalArgumentException} for an unsupported carrier.
     */
    @Test
    @DisplayName("uploadMappings - Should throw exception for unsupported carrier")
    void uploadMappings_withUnsupportedCarrier_shouldThrowException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                mappingService.uploadMappings(file, "user", "unsupported_carrier")
        );
        assertEquals("Unsupported carrier for department mapping upload: unsupported_carrier", ex.getMessage());
    }

    /**
     * Tests the edge case where all records in the uploaded CSV are duplicates.
     * It should return an empty list and not call the saveAll method.
     *
     * @throws IOException if file processing fails.
     */
    @Test
    @DisplayName("uploadMappings - Should return empty list if all records are duplicates")
    void uploadMappings_withAllDuplicateData_shouldReturnEmptyList() throws IOException {
        // Arrange
        String content = "FAN,ACCOUNT #,DEPT\nFAN1,ACC123,Dept1";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", content.getBytes());

        when(mappingDao.existsByDepartmentAccountNumberAndIsDeletedFalse("ACC123")).thenReturn(true);

        // Act
        List<AccountDepartmentMapping> result = mappingService.uploadMappings(file, "testUser", "firstnet");

        // Assert
        assertThat(result).isEmpty();
        verify(mappingDao, never()).saveAll(anyList());
    }
}
