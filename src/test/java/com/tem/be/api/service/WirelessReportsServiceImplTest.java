package com.tem.be.api.service;

import com.tem.be.api.dao.ATTInvoiceDao;
import com.tem.be.api.dao.FirstNetInvoiceDao;
import com.tem.be.api.dao.VerizonWirelessInvoiceDao;
import com.tem.be.api.dao.WirelessReportsDao;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierDistinctDTO;
import com.tem.be.api.dto.dashboard.DepartmentCarrierPlansDTO;
import com.tem.be.api.dto.dashboard.DepartmentTotalDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.WirelessReports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link WirelessReportsServiceImpl} class.
 * <p>
 * This class isolates the service layer logic by mocking all DAO dependencies
 * ({@link WirelessReportsDao}, {@link FirstNetInvoiceDao}, {@link ATTInvoiceDao}, and
 * {@link VerizonWirelessInvoiceDao}). Each test verifies a specific business rule,
 * such as data retrieval, report updates, date range filtering, and carrier-specific
 * data aggregation, ensuring the service methods behave as expected.
 */
@ExtendWith(MockitoExtension.class)
class WirelessReportsServiceImplTest {

    /**
     * Mocked DAO for general wireless report operations.
     */
    @Mock
    private WirelessReportsDao wirelessReportsDao;

    /**
     * Mocked DAO for FirstNet invoice data.
     */
    @Mock
    private FirstNetInvoiceDao firstNetInvoiceDao;

    /**
     * Mocked DAO for AT&T invoice data.
     */
    @Mock
    private ATTInvoiceDao attInvoiceDao;

    /**
     * Mocked DAO for Verizon Wireless invoice data.
     */
    @Mock
    private VerizonWirelessInvoiceDao verizonWirelessInvoiceDao;

    /**
     * The instance of the service being tested, with mocked dependencies injected.
     */
    @InjectMocks
    private WirelessReportsServiceImpl wirelessReportsService;

    // Reusable test data objects
    private WirelessReports report1;
    private ReportUpdateDTO reportUpdateDTO;
    private Date startDate;
    private Date endDate;

    /**
     * Sets up common test data before each test execution. This ensures a consistent
     * starting state for each test case.
     */
    @BeforeEach
    void setUp() {
        report1 = new WirelessReports();
        report1.setId(1L);
        report1.setDepartment("IT");
        report1.setCarrier("Verizon");

        reportUpdateDTO = new ReportUpdateDTO();
        reportUpdateDTO.setDepartment("Finance");
        reportUpdateDTO.setViscode("V123");
        reportUpdateDTO.setNameOnInvoice("John Doe Updated");

        startDate = new Date();
        endDate = new Date();
    }

    /**
     * Tests that {@code getAllWirelessReports} successfully retrieves and returns all reports from the DAO.
     */
    @Test
    @DisplayName("getAllWirelessReports - Should return a list of all reports")
    void getAllWirelessReports_shouldReturnAllReports() {
        // Arrange
        when(wirelessReportsDao.findAll()).thenReturn(List.of(report1));

        // Act
        List<WirelessReports> reports = wirelessReportsService.getAllWirelessReports();

        // Assert
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getDepartment()).isEqualTo("IT");
        verify(wirelessReportsDao).findAll();
    }

    /**
     * Tests that {@code getDistinctDepartmentAndCarrier} successfully retrieves and aggregates
     * distinct department names, carrier names, and device classes into a DTO.
     */
    @Test
    @DisplayName("getDistinctDepartmentAndCarrier - Should return DTO with distinct values")
    void getDistinctDepartmentAndCarrier_shouldReturnDto() {
        // Arrange
        when(wirelessReportsDao.findDistinctDepartments()).thenReturn(List.of("IT"));
        when(wirelessReportsDao.findDistinctCarriers()).thenReturn(List.of("Verizon"));
        when(wirelessReportsDao.findDistinctDeviceClasses()).thenReturn(List.of("Phone"));

        // Act
        DepartmentCarrierDistinctDTO result = wirelessReportsService.getDistinctDepartmentAndCarrier();

        // Assert
        assertThat(result.getDepartments()).hasSize(1).contains("IT");
        assertThat(result.getCarriers()).hasSize(1).contains("Verizon");
        assertThat(result.getDeviceClasses()).hasSize(1).contains("Phone");
        verify(wirelessReportsDao).findDistinctDepartments();
        verify(wirelessReportsDao).findDistinctCarriers();
        verify(wirelessReportsDao).findDistinctDeviceClasses();
    }

    /**
     * Tests that {@code filterWirelessReports} calls the correct DAO method when both department
     * and carrier filters are provided.
     */
    @Test
    @DisplayName("filterWirelessReports - Should call findByDepartmentsAndCarriers when both are provided")
    void filterWirelessReports_withBothFilters_shouldCallCorrectDaoMethod() {
        // Arrange
        List<String> depts = List.of("IT");
        List<String> carriers = List.of("Verizon");
        when(wirelessReportsDao.findByDepartmentsAndCarriers(depts, carriers)).thenReturn(List.of(report1));

        // Act
        wirelessReportsService.filterWirelessReports(depts, carriers);

        // Assert
        verify(wirelessReportsDao).findByDepartmentsAndCarriers(depts, carriers);
    }

    /**
     * Tests that {@code updateById} successfully finds, updates, and persists the report
     * when a valid ID is provided.
     */
    @Test
    @DisplayName("updateById - Should update and return the report when found")
    void updateById_whenFound_shouldUpdateAndReturnReport() {
        // Arrange
        when(wirelessReportsDao.findById(1L)).thenReturn(Optional.of(report1));
        when(wirelessReportsDao.save(any(WirelessReports.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WirelessReports updatedReport = wirelessReportsService.updateById(1L, reportUpdateDTO);

        // Assert
        assertThat(updatedReport).isNotNull();
        assertThat(updatedReport.getDepartment()).isEqualTo("Finance");
        assertThat(updatedReport.getViscode()).isEqualTo("V123");
        verify(wirelessReportsDao).findById(1L);
        verify(wirelessReportsDao).save(report1);
    }

    /**
     * Tests that {@code updateById} throws a {@link ResourceNotFoundException} when the
     * report ID does not exist in the database.
     */
    @Test
    @DisplayName("updateById - Should throw ResourceNotFoundException when not found")
    void updateById_whenNotFound_shouldThrowException() {
        // Arrange
        long nonExistentId = 99L;
        when(wirelessReportsDao.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                wirelessReportsService.updateById(nonExistentId, reportUpdateDTO)
        );

        assertEquals("Wireless report not found with ID: " + nonExistentId, exception.getMessage());
        verify(wirelessReportsDao, never()).save(any());
    }

    /**
     * Tests that {@code getExpenseSummaryByCarrier} correctly delegates to the {@code FirstNetInvoiceDao}
     * and correctly maps the raw data when the carrier is 'firstnet'.
     */
    @Test
    @DisplayName("getExpenseSummaryByCarrier - Should call FirstNet DAO for 'firstnet' carrier")
    void getExpenseSummaryByCarrier_forFirstNet_shouldCallCorrectDao() {
        // Arrange
        String carrier = "firstnet";
        Object[] rawRow = {"IT", "User1", "1112223333", new BigDecimal("120.50")};
        when(firstNetInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate)).thenReturn(Collections.singletonList(rawRow));

        // Act
        var result = wirelessReportsService.getExpenseSummaryByCarrier(carrier, startDate, endDate);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo("IT");
        assertThat(result.get(0).getTotal()).isEqualByComparingTo(new BigDecimal("120.50"));
        verify(firstNetInvoiceDao).findExpenseSummaryByDateRange(startDate, endDate);
        verifyNoInteractions(attInvoiceDao, verizonWirelessInvoiceDao);
    }

    /**
     * Tests that the raw data processing logic handles charge values provided as strings (e.g., "$55.25")
     * by successfully removing the currency symbol and converting to {@code BigDecimal}.
     */
    @Test
    @DisplayName("getExpenseSummaryByCarrier - Should correctly parse string charge value")
    void processRawExpenseData_shouldParseStringCharge() {
        // Arrange
        String carrier = "firstnet";
        Object[] rawRow = {"Sales", "User2", "4445556666", "$55.25"};
        when(firstNetInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate)).thenReturn(Collections.singletonList(rawRow));

        // Act
        var result = wirelessReportsService.getExpenseSummaryByCarrier(carrier, startDate, endDate);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotal()).isEqualByComparingTo(new BigDecimal("55.25"));
    }

    /**
     * Tests that {@code getDepartmentWiseCarrierPlans} correctly processes the raw data from the DAO,
     * aggregating plans and totals by department, including splitting plans into individual counts.
     */
    @Test
    @DisplayName("getDepartmentWiseCarrierPlans - Should correctly group plans by department and carrier")
    void getDepartmentWiseCarrierPlans_shouldGroupCorrectly() {
        // Arrange: Simulate raw DAO output with multiple entries for the same department
        Object[] rawRow1 = {"IT", "Verizon", "Plan A,Plan B"};
        Object[] rawRow2 = {"IT", "AT&T", "Plan C"};
        Object[] rawRow3 = {"Sales", "Verizon", "Plan D"};
        List<Object[]> rawData = List.of(rawRow1, rawRow2, rawRow3);
        when(wirelessReportsDao.getGroupedPlansByDepartmentAndCarrier()).thenReturn(rawData);

        // Act
        List<DepartmentCarrierPlansDTO> result = wirelessReportsService.getDepartmentWiseCarrierPlans();

        // Assert
        assertThat(result).hasSize(2);

        // Assert IT Department
        DepartmentCarrierPlansDTO itDept = result.stream().filter(d -> d.getDepartment().equals("IT")).findFirst().orElse(null);
        assertThat(itDept).isNotNull();
        assertThat(itDept.getTotalPlans()).isEqualTo(3);
        assertThat(itDept.getCarriers()).hasSize(2);

        // Assert Sales Department
        DepartmentCarrierPlansDTO salesDept = result.stream().filter(d -> d.getDepartment().equals("Sales")).findFirst().orElse(null);
        assertThat(salesDept).isNotNull();
        assertThat(salesDept.getTotalPlans()).isEqualTo(1);
        verify(wirelessReportsDao).getGroupedPlansByDepartmentAndCarrier();
    }

    /**
     * Tests that {@code getCarrierTotalsByRange} correctly aggregates total charges by department
     * across different carriers from the raw DAO data.
     */
    @Test
    @DisplayName("getCarrierTotalsByRange - Should correctly calculate department totals")
    void getCarrierTotalsByRange_shouldCalculateCorrectly() {
        // Arrange: Simulate raw DAO output with charges for two departments
        Object[] rawRow1 = {"IT", "Verizon", "ACC1", new BigDecimal("100.50")};
        Object[] rawRow2 = {"IT", "AT&T", "ACC2", new BigDecimal("50.00")};
        Object[] rawRow3 = {"Sales", "Verizon", "ACC3", new BigDecimal("200.00")};
        List<Object[]> rawData = List.of(rawRow1, rawRow2, rawRow3);
        when(wirelessReportsDao.getCarrierTotalsBetweenDates(startDate, endDate)).thenReturn(rawData);

        // Act
        List<DepartmentTotalDTO> result = wirelessReportsService.getCarrierTotalsByRange(startDate, endDate);

        // Assert
        assertThat(result).hasSize(2);

        // Assert IT Department Total (100.50 + 50.00 = 150.50)
        DepartmentTotalDTO itDept = result.stream().filter(d -> d.getDepartment().equals("IT")).findFirst().orElse(null);
        assertThat(itDept).isNotNull();
        assertThat(itDept.getTotal()).isEqualByComparingTo(new BigDecimal("150.50"));

        verify(wirelessReportsDao).getCarrierTotalsBetweenDates(startDate, endDate);
    }

    /**
     * Tests that {@code getFilteredReports} correctly calls the DAO's generalized filtering method
     * with all provided non-null parameters.
     */
    @Test
    @DisplayName("getFilteredReports - Should call DAO with all provided filters")
    void getFilteredReports_shouldCallDaoWithFilters() {
        // Arrange
        String department = "IT";
        String carrier = "Verizon";
        when(wirelessReportsDao.findWithOptionalFilters(startDate, endDate, department, carrier)).thenReturn(List.of(report1));

        // Act
        wirelessReportsService.getFilteredReports(startDate, endDate, department, carrier);

        // Assert
        verify(wirelessReportsDao).findWithOptionalFilters(startDate, endDate, department, carrier);
    }

    /**
     * Tests that {@code getExpenseSummaryByCarrier} throws an {@link IllegalArgumentException}
     * when an unsupported or unrecognized carrier name is provided.
     */
    @Test
    @DisplayName("getExpenseSummaryByCarrier - Should throw IllegalArgumentException for unsupported carrier")
    void getExpenseSummaryByCarrier_forUnsupportedCarrier_shouldThrowException() {
        // Arrange
        String carrier = "unsupported_carrier";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                wirelessReportsService.getExpenseSummaryByCarrier(carrier, startDate, endDate)
        );

        assertEquals("Unsupported or unknown carrier: " + carrier, exception.getMessage());
        verifyNoInteractions(firstNetInvoiceDao, attInvoiceDao, verizonWirelessInvoiceDao);
    }
}