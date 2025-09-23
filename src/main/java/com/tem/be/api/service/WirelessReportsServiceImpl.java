package com.tem.be.api.service;

import com.tem.be.api.dao.ATTInvoiceDao;
import com.tem.be.api.dao.FirstNetInvoiceDao;
import com.tem.be.api.dao.WirelessReportsDao;
import com.tem.be.api.dto.dashboard.*;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.TotalByCarrierDTO;
import com.tem.be.api.dto.TotalByDepartmentDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.WirelessReports;
import com.tem.be.api.utils.NetworkProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.tem.be.api.utils.NetworkProvider.FIRSTNET;

/**
 * Service for wireless reports related operations.
 */
@Service
@Transactional
@Log4j2
public class WirelessReportsServiceImpl implements WirelessReportsService {

    private final WirelessReportsDao wirelessReportsDao;
    private final FirstNetInvoiceDao firstNetInvoiceDao;
    private final ATTInvoiceDao aTTInvoiceDao;

    @Autowired
    public WirelessReportsServiceImpl(WirelessReportsDao wirelessReportsDao, FirstNetInvoiceDao firstNetInvoiceDao, ATTInvoiceDao aTTInvoiceDao) {
        this.wirelessReportsDao = wirelessReportsDao;
        this.firstNetInvoiceDao = firstNetInvoiceDao;
        this.aTTInvoiceDao = aTTInvoiceDao;
    }

    @Override
    public List<WirelessReports> getAllWirelessReports() {
        return wirelessReportsDao.findAll();
    }

    @Override
    public DepartmentCarrierDistinctDTO getDistinctDepartmentAndCarrier() {
        List<String> departments = wirelessReportsDao.findDistinctDepartments();
        List<String> carriers = wirelessReportsDao.findDistinctCarriers();
        List<String> deviceClasses = wirelessReportsDao.findDistinctDeviceClasses();

        return new DepartmentCarrierDistinctDTO(departments, carriers, deviceClasses);
    }

    @Override
    public List<WirelessReports> filterWirelessReports(List<String> departments, List<String> carriers) {
        boolean hasDepartments = departments != null && !departments.isEmpty();
        boolean hasCarriers = carriers != null && !carriers.isEmpty();

        if (hasDepartments && hasCarriers) {
            return wirelessReportsDao.findByDepartmentsAndCarriers(departments, carriers);
        } else if (hasDepartments) {
            return wirelessReportsDao.findByDepartments(departments);
        } else if (hasCarriers) {
            return wirelessReportsDao.findByCarriers(carriers);
        } else {
            return wirelessReportsDao.findAll(); // No filters
        }
    }

    @Override
    public WirelessReports updateById(Long id, ReportUpdateDTO dto) {
        WirelessReports existingTelecom = wirelessReportsDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wireless report not found with ID: " + id));
        existingTelecom.setDepartment(dto.getDepartment());
        existingTelecom.setViscode(dto.getViscode());
        existingTelecom.setNameOnInvoice(dto.getNameOnInvoice());
        return wirelessReportsDao.save(existingTelecom);
    }

    @Override
    public List<TotalByDepartmentDTO> getTotalByDepartment() {
        List<Object[]> rawResults = wirelessReportsDao.getTotalByDepartment();
        List<TotalByDepartmentDTO> results = new ArrayList<>();

        for (Object[] row : rawResults) {
            String dept = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            results.add(new TotalByDepartmentDTO(dept, total));
        }
        return results;
    }

    @Override
    public List<TotalByCarrierDTO> totalByCarrier() {
        List<Object[]> rawResults = wirelessReportsDao.totalByCarrier();
        List<TotalByCarrierDTO> results = new ArrayList<>();

        for (Object[] row : rawResults) {
            String dept = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            results.add(new TotalByCarrierDTO(dept, total));
        }
        return results;
    }

    @Override
    public List<WirelessReports> getReportsBetweenDates(Date startDate, Date endDate) {
        return wirelessReportsDao.findByLastInvoiceOnBetween(startDate, endDate);
    }

    @Override
    public List<DepartmentCarrierPlansDTO> getDepartmentWiseCarrierPlans() {
        List<Object[]> rawData = wirelessReportsDao.getGroupedPlansByDepartmentAndCarrier();

        Map<String, List<PlanCarrierDTO>> departmentMap = new HashMap<>();

        for (Object[] row : rawData) {
            String department = (String) row[0];
            String carrier = (String) row[1];
            String[] planArray = ((String) row[2]).split(",");
            List<String> plans = Arrays.stream(planArray).distinct().toList();
            departmentMap
                    .computeIfAbsent(department, d -> new ArrayList<>())
                    .add(new PlanCarrierDTO(carrier, plans));
        }

        List<DepartmentCarrierPlansDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<PlanCarrierDTO>> entry : departmentMap.entrySet()) {
            int totalPlans = entry.getValue().stream()
                    .mapToInt(dto -> dto.getPlans().size())
                    .sum();
            result.add(new DepartmentCarrierPlansDTO(entry.getKey(), entry.getValue(), totalPlans));
        }
        return result;
    }

    @Override
    public List<DepartmentTotalDTO> getCarrierTotalsByRange(Date startDate, Date endDate) {
        List<Object[]> rawData = wirelessReportsDao.getCarrierTotalsBetweenDates(startDate, endDate);

        Map<String, List<CarrierTotalDTO>> departmentMap = new HashMap<>();
        Map<String, BigDecimal> departmentTotalMap = new HashMap<>();

        for (Object[] row : rawData) {
            String department = (String) row[0];
            String carrier = (String) row[1];
            String account = (String) row[2];
            BigDecimal total = (BigDecimal) row[3];

            CarrierTotalDTO carrierDTO = new CarrierTotalDTO(carrier, account, total);

            departmentMap
                    .computeIfAbsent(department, d -> new ArrayList<>())
                    .add(carrierDTO);

            departmentTotalMap.merge(department, total, BigDecimal::add);
        }

        return departmentMap.entrySet().stream()
                .map(entry -> {
                    String deptName = entry.getKey();
                    List<CarrierTotalDTO> carrierTotals = entry.getValue();
                    BigDecimal departmentTotal = departmentTotalMap.get(deptName);
                    return new DepartmentTotalDTO(deptName, carrierTotals, departmentTotal);
                })
                .toList();
    }

    @Override
    public List<WirelessReports> getFilteredReports(Date startDate, Date endDate, String department, String carrier) {
        return wirelessReportsDao.findWithOptionalFilters(startDate, endDate, department, carrier);
    }

    /**
     * Fetches and processes a summary of expenses for a given carrier and date range,
     * grouping the results by department.
     *
     * @param carrier   The network provider (e.g., "firstnet").
     * @param startDate The start date of the reporting period.
     * @param endDate   The end date of the reporting period.
     * @return A list of {@link DepartmentExpenseDTO} objects, each representing a department's summary.
     * @throws IllegalArgumentException if the carrier is unsupported or unknown.
     */
    @Override
    public List<DepartmentExpenseDTO> getExpenseSummaryByCarrier(String carrier, Date startDate, Date endDate) {
        log.info("Starting expense summary generation for carrier: {}, from: {} to: {}", carrier, startDate, endDate);

        // 1. Identify the provider using your helper method
        NetworkProvider provider = identifyProvider(carrier);

        // 2. Use a modern 'switch' expression on the enum to fetch data
        List<Object[]> rawData = switch (provider) {
            case FIRSTNET -> firstNetInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate);
            case ATT -> aTTInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate);
            case VERIZON -> Collections.emptyList();
            case UNKNOWN -> throw new IllegalArgumentException("Unsupported or unknown carrier specified: " + carrier);
        };

        log.info("Fetched {} raw records from the database for provider: {}", rawData.size(), provider);

        // 3. Process the retrieved data (this method is already well-written)
        return processRawExpenseData(rawData);
    }

    /**
     * Identifies the {@link NetworkProvider} enum from a raw carrier string.
     * This method is case-insensitive.
     *
     * @param carrier The raw carrier name string (e.g., "firstnet", "AT&T").
     * @return The corresponding {@link NetworkProvider} enum, or {@code UNKNOWN} if no match is found.
     */
    private NetworkProvider identifyProvider(String carrier) {
        if (carrier == null || carrier.isBlank()) {
            return NetworkProvider.UNKNOWN;
        }
        String lowerCaseCarrier = carrier.toLowerCase();
        if (lowerCaseCarrier.contains("firstnet")) {
            return NetworkProvider.FIRSTNET;
        } else if (lowerCaseCarrier.contains("at&t mobility")) {
            return NetworkProvider.ATT;
        } else if (lowerCaseCarrier.contains("verizon")) {
            return NetworkProvider.VERIZON;
        }
        return NetworkProvider.UNKNOWN;
    }

    /**
     * Processes raw data from a native SQL query into a structured list of {@link DepartmentExpenseDTO}s.
     * This method groups invoice records by department, cleans and parses currency strings, calculates
     * the total for each department, and formats the final output.
     *
     * @param rawData A list of {@code Object[]} where each array represents a row from the database,
     *                expected to contain [department, wireless_number, total_current_charges].
     * @return A structured list of {@link DepartmentExpenseDTO}s ready for the API response.
     */
    private List<DepartmentExpenseDTO> processRawExpenseData(List<Object[]> rawData) {
        log.debug("Starting to process {} raw data rows into DTOs.", rawData.size());
        Map<String, DepartmentExpenseDTO> departmentMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            String department = (String) row[0];
            String userName = (String) row[1];
            String wirelessNumber = (String) row[2];
            String totalChargesStr = (String) row[3];

            if (department == null || wirelessNumber == null || totalChargesStr == null) {
                log.warn("Skipping incomplete record: [dept={}, num={}, charges={}]", department, wirelessNumber, totalChargesStr);
                continue;
            }

            BigDecimal total;
            try {
                // Clean the string by removing any non-digit and non-decimal point characters (e.g., '$', ',')
                String cleanedTotalStr = totalChargesStr.replaceAll("[^\\d.]", "");
                if (cleanedTotalStr.isEmpty()) {
                    total = BigDecimal.ZERO;
                } else {
                    total = new BigDecimal(cleanedTotalStr);
                }
            } catch (NumberFormatException e) {
                log.error("Could not parse total_current_charges '{}' for wireless number {}. Defaulting to ZERO.", totalChargesStr, wirelessNumber, e);
                total = BigDecimal.ZERO;
            }

            ExpenseInvoiceDTO invoiceDTO = new ExpenseInvoiceDTO(userName, wirelessNumber, total);

            DepartmentExpenseDTO deptDto = departmentMap.computeIfAbsent(department, k ->
                    new DepartmentExpenseDTO(k, new ArrayList<>(), BigDecimal.ZERO)
            );

            deptDto.getInvoices().add(invoiceDTO);
            deptDto.setTotal(deptDto.getTotal().add(total));
        }

        log.debug("Finished processing. Created {} department DTOs.", departmentMap.size());
        return new ArrayList<>(departmentMap.values());
    }
}
