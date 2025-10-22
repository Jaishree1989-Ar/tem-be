package com.tem.be.api.service;

import com.tem.be.api.dao.ATTInvoiceDao;
import com.tem.be.api.dao.FirstNetInvoiceDao;
import com.tem.be.api.dao.VerizonWirelessInvoiceDao;
import com.tem.be.api.dao.WirelessReportsDao;
import com.tem.be.api.dto.dashboard.*;
import com.tem.be.api.dto.ReportUpdateDTO;
import com.tem.be.api.dto.TotalByCarrierDTO;
import com.tem.be.api.dto.TotalByDepartmentDTO;
import com.tem.be.api.exception.ResourceNotFoundException;
import com.tem.be.api.model.WirelessReports;
import com.tem.be.api.utils.CarrierConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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
    private final VerizonWirelessInvoiceDao verizonWirelessInvoiceDao;

    @Autowired
    public WirelessReportsServiceImpl(WirelessReportsDao wirelessReportsDao,
                                      FirstNetInvoiceDao firstNetInvoiceDao,
                                      ATTInvoiceDao aTTInvoiceDao,
                                      VerizonWirelessInvoiceDao verizonWirelessInvoiceDao) {
        this.wirelessReportsDao = wirelessReportsDao;
        this.firstNetInvoiceDao = firstNetInvoiceDao;
        this.aTTInvoiceDao = aTTInvoiceDao;
        this.verizonWirelessInvoiceDao = verizonWirelessInvoiceDao;
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

        List<Object[]> rawData = switch (carrier.toLowerCase()) {
            case CarrierConstants.FIRSTNET_LC -> firstNetInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate);
            case CarrierConstants.ATT_LC -> aTTInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate);
            case CarrierConstants.VERIZON_WIRELESS_LC ->
                    verizonWirelessInvoiceDao.findExpenseSummaryByDateRange(startDate, endDate);
            default -> throw new IllegalArgumentException("Unsupported or unknown carrier: " + carrier);
        };

        log.info("Fetched {} raw records from the database for carrier: {}", rawData.size(), carrier);
        return processRawExpenseData(rawData);
    }

    /**
     * Processes raw data from a database query into a structured list of {@link DepartmentExpenseDTO}s.
     * This method groups invoice records by department, delegates charge parsing, calculates
     * the total for each department, and formats the final output.
     *
     * @param rawData A list of {@code Object[]} where each array represents a row from the database.
     * @return A structured list of {@link DepartmentExpenseDTO}s ready for the API response.
     */
    private List<DepartmentExpenseDTO> processRawExpenseData(List<Object[]> rawData) {
        log.debug("Starting to process {} raw data rows into DTOs.", rawData.size());
        Map<String, DepartmentExpenseDTO> departmentMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            String department = (String) row[0];
            String userName = (String) row[1];
            String wirelessNumber = (String) row[2];
            Object chargesObject = row[3];

            if (department == null || wirelessNumber == null) {
                log.warn("Skipping record with null department or wireless number.");
                continue;
            }

            BigDecimal total = parseChargeValue(chargesObject, wirelessNumber);

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


    /**
     * Parses a charge value from a raw object, handling different data types (BigDecimal, String).
     *
     * @param chargesObject The object containing the charge value, which could be null.
     * @param identifier    A unique identifier (like a wireless number) for logging purposes.
     * @return The parsed BigDecimal value, or BigDecimal.ZERO if parsing fails or input is invalid.
     */
    private BigDecimal parseChargeValue(Object chargesObject, String identifier) {
        if (chargesObject == null) {
            return BigDecimal.ZERO;
        }

        if (chargesObject instanceof BigDecimal totalCharges) {
            return totalCharges;
        }

        if (chargesObject instanceof String totalChargesStr) {
            try {
                String cleanedTotalStr = totalChargesStr.replaceAll("[^\\d.-]", "");
                if (cleanedTotalStr.isEmpty() || cleanedTotalStr.equals("-")) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(cleanedTotalStr);
            } catch (NumberFormatException e) {
                log.error("Could not parse total_current_charges string '{}' for identifier {}. Defaulting to ZERO.", totalChargesStr, identifier, e);
                return BigDecimal.ZERO;
            }
        }

        log.warn("Unexpected data type '{}' for charges for identifier {}. Defaulting to ZERO.", chargesObject.getClass().getName(), identifier);
        return BigDecimal.ZERO;
    }
}
