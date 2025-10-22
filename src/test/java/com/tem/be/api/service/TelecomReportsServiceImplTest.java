package com.tem.be.api.service;

import com.tem.be.api.dao.TelecomReportsDao;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.TelecomReports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelecomReportsServiceImplTest {

    @Mock
    private TelecomReportsDao telecomReportsDao;

    @InjectMocks
    private TelecomReportsServiceImpl telecomReportsService;

    private TelecomReports report1;
    private ReportUpdateDTO reportUpdateDTO;

    @BeforeEach
    void setUp() {
        report1 = new TelecomReports();
        report1.setId(1L);
        report1.setDepartment("IT");

        reportUpdateDTO = new ReportUpdateDTO();
        reportUpdateDTO.setDepartment("Finance");
        reportUpdateDTO.setViscode("V123");
    }

    /**
     * Tests the service method to retrieve all telecom reports.
     * Verifies that the correct DAO method is called and returns the expected list.
     */
    @Test
    @DisplayName("getAllTelecomReports - Should return all reports from DAO")
    void getAllTelecomReports_shouldReturnAllReports() {
        // Arrange
        when(telecomReportsDao.findAll()).thenReturn(List.of(report1));

        // Act
        List<TelecomReports> reports = telecomReportsService.getAllTelecomReports();

        // Assert
        assertThat(reports)
                .as("The list of reports should not be null and contain the expected number of items.")
                .isNotNull()
                .hasSize(1);

        verify(telecomReportsDao).findAll();
    }

    /**
     * Tests the update functionality when a report with the given ID is found.
     * Verifies that the report fields are updated and the DAO's save method is called.
     */
    @Test
    @DisplayName("updateById - Should update and return the report when found")
    void updateById_whenFound_shouldUpdateAndReturn() {
        // Arrange
        when(telecomReportsDao.findById(1L)).thenReturn(Optional.of(report1));
        when(telecomReportsDao.save(any(TelecomReports.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TelecomReports updatedReport = telecomReportsService.updateById(1L, reportUpdateDTO);

        // Assert
        assertThat(updatedReport).isNotNull();
        assertThat(updatedReport.getDepartment()).isEqualTo("Finance");
        assertThat(updatedReport.getViscode()).isEqualTo("V123");
        verify(telecomReportsDao).findById(1L);
        verify(telecomReportsDao).save(any(TelecomReports.class));
    }

    /**
     * Tests the update functionality when a report with the given ID is not found.
     * Verifies that a {@link ResourceNotFoundException} is thrown and the save method is never called.
     */
    @Test
    @DisplayName("updateById - Should throw ResourceNotFoundException when not found")
    void updateById_whenNotFound_shouldThrowException() {
        // Arrange
        long nonExistentId = 99L;
        when(telecomReportsDao.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            telecomReportsService.updateById(nonExistentId, reportUpdateDTO);
        });
        assertEquals("Telecom report not found with ID: " + nonExistentId, exception.getMessage());
        verify(telecomReportsDao, never()).save(any());
    }

    /**
     * Tests the retrieval of distinct department, carrier, and device class values.
     * Verifies that the correct DAO methods are called and the service returns a populated DTO.
     */
    @Test
    @DisplayName("getDistinctDepartmentAndCarrier - Should return DTO with distinct values")
    void getDistinctDepartmentAndCarrier_shouldReturnDto() {
        // Arrange
        when(telecomReportsDao.findDistinctDepartments()).thenReturn(List.of("Sales"));
        when(telecomReportsDao.findDistinctCarriers()).thenReturn(List.of("Comcast"));
        when(telecomReportsDao.findDistinctDeviceClasses()).thenReturn(List.of("Desk Phone"));

        // Act
        DepartmentCarrierDistinctDTO result = telecomReportsService.getDistinctDepartmentAndCarrier();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDepartments()).containsExactly("Sales");
        assertThat(result.getCarriers()).containsExactly("Comcast");
        assertThat(result.getDeviceClasses()).containsExactly("Desk Phone");
    }

    /**
     * Tests the filtering logic when both departments and carriers are provided.
     * Verifies that {@code findByDepartmentsAndCarriers} is called and other find methods are not.
     */
    @Test
    @DisplayName("telecomReportsService - Should call findByDepartmentsAndCarriers when both are present")
    void telecomReportsService_withBothFilters_shouldCallCorrectDaoMethod() {
        // Arrange
        List<String> departments = List.of("Sales");
        List<String> carriers = List.of("Verizon");

        // Act
        telecomReportsService.telecomReportsService(departments, carriers);

        // Assert
        verify(telecomReportsDao).findByDepartmentsAndCarriers(departments, carriers);
        verify(telecomReportsDao, never()).findByDepartments(any());
        verify(telecomReportsDao, never()).findByCarriers(any());
        verify(telecomReportsDao, never()).findAll();
    }

    /**
     * Tests the filtering logic when only departments are provided.
     * Verifies that {@code findByDepartments} is called.
     */
    @Test
    @DisplayName("telecomReportsService - Should call findByDepartments when only departments are present")
    void telecomReportsService_withOnlyDepartments_shouldCallCorrectDaoMethod() {
        // Arrange
        List<String> departments = List.of("Sales");

        // Act
        telecomReportsService.telecomReportsService(departments, null);

        // Assert
        verify(telecomReportsDao).findByDepartments(departments);
        verify(telecomReportsDao, never()).findByDepartmentsAndCarriers(any(), any());
    }

    /**
     * Tests the filtering logic when no filters (departments or carriers) are provided.
     * Verifies that {@code findAll} is called to return all reports.
     */
    @Test
    @DisplayName("telecomReportsService - Should call findAll when no filters are present")
    void telecomReportsService_withNoFilters_shouldCallFindAll() {
        // Act
        telecomReportsService.telecomReportsService(null, null);

        // Assert
        verify(telecomReportsDao).findAll();
        verify(telecomReportsDao, never()).findByDepartments(any());
        verify(telecomReportsDao, never()).findByCarriers(any());
    }
}