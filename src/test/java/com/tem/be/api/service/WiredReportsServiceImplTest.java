package com.tem.be.api.service;

import com.tem.be.api.dao.WiredReportsDao;
import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.WiredReports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link WiredReportsServiceImpl} class.
 * <p>
 * This class isolates the service layer logic by mocking the {@link WiredReportsDao} dependency.
 * Each test verifies a specific business rule, such as searching with filters and updating records,
 * ensuring the service methods behave as expected.
 */
@ExtendWith(MockitoExtension.class)
class WiredReportsServiceImplTest {

    /**
     * Mocked DAO for wired report operations.
     */
    @Mock
    private WiredReportsDao wiredReportsDao;

    /**
     * The instance of the service being tested, with mocked dependencies injected.
     */
    @InjectMocks
    private WiredReportsServiceImpl wiredReportsService;

    private WiredReports report;

    /**
     * Sets up common test data before each test execution. This ensures a consistent
     * starting state for each test case.
     */
    @BeforeEach
    void setUp() {
        report = new WiredReports();
        report.setId(1L);
        report.setCarrier("CALNET");
        report.setViscode("V100");
    }

    /**
     * Tests that {@code searchWiredReports} calls the DAO's findAll method with a specification
     * created from a filter that includes a carrier and a date range.
     */
    @Test
    @DisplayName("searchWiredReports - Should call DAO with Specification for a filter with carrier and date range")
    void searchWiredReports_shouldCallDaoWithSpecification() {
        // Arrange
        WiredReportsFilterDto filterDto = new WiredReportsFilterDto();
        filterDto.setCarrier("CALNET");
        filterDto.setStartDate(LocalDate.of(2024, 1, 1));
        filterDto.setEndDate(LocalDate.of(2024, 1, 31));

        when(wiredReportsDao.findAll(any(Specification.class))).thenReturn(List.of(report));

        // Act
        List<WiredReports> results = wiredReportsService.searchWiredReports(filterDto);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCarrier()).isEqualTo("CALNET");
        verify(wiredReportsDao).findAll(any(Specification.class));
    }

    /**
     * Tests that {@code updateViscode} successfully finds, updates, and persists the report
     * for a supported carrier ("calnet").
     */
    @Test
    @DisplayName("updateViscode - Should update and return the report for a supported carrier")
    void updateViscode_whenFound_shouldUpdateAndReturnReport() {
        // Arrange
        String newViscode = "V250";
        String carrier = "calnet";
        when(wiredReportsDao.findById(1L)).thenReturn(Optional.of(report));
        when(wiredReportsDao.save(any(WiredReports.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Object updatedReportObj = wiredReportsService.updateViscode(1L, newViscode, carrier);
        WiredReports updatedReport = (WiredReports) updatedReportObj;

        // Assert
        assertThat(updatedReport).isNotNull();
        assertThat(updatedReport.getViscode()).isEqualTo(newViscode);
        verify(wiredReportsDao).findById(1L);
        verify(wiredReportsDao).save(report);
    }

    /**
     * Tests that {@code updateViscode} throws an {@link EntityNotFoundException} when the
     * report ID does not exist in the database.
     */
    @Test
    @DisplayName("updateViscode - Should throw EntityNotFoundException when report is not found")
    void updateViscode_whenNotFound_shouldThrowException() {
        // Arrange
        long nonExistentId = 99L;
        String carrier = "calnet";
        when(wiredReportsDao.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                wiredReportsService.updateViscode(nonExistentId, "V300", carrier)
        );

        assertEquals("WiredReports not found with ID: " + nonExistentId, exception.getMessage());
        verify(wiredReportsDao, never()).save(any());
    }

    /**
     * Tests that {@code updateViscode} throws an {@link IllegalArgumentException}
     * when an unsupported or unrecognized carrier name is provided.
     */
    @Test
    @DisplayName("updateViscode - Should throw IllegalArgumentException for an unsupported carrier")
    void updateViscode_forUnsupportedCarrier_shouldThrowException() {
        // Arrange
        String carrier = "unsupported_carrier";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                wiredReportsService.updateViscode(1L, "V400", carrier)
        );

        assertEquals("Unsupported or unknown carrier: " + carrier, exception.getMessage());
        verify(wiredReportsDao, never()).findById(anyLong());
        verify(wiredReportsDao, never()).save(any());
    }
}