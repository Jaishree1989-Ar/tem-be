package com.tem.be.api.service;

import com.tem.be.api.dto.WiredReportsFilterDto;
import com.tem.be.api.model.WiredReports;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class WiredReportsSpecifications {

    private WiredReportsSpecifications() {
    }

    /**
     * Creates a JPA Specification based on the dynamic filter criteria.
     *
     * @param filter The DTO containing all filter parameters.
     * @return A composed Specification for querying the WiredReports entity.
     */
    public static Specification<WiredReports> findByCriteria(WiredReportsFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filter by 'carrier' if it is provided
            if (filter.getCarrier() != null && !filter.getCarrier().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("carrier"), filter.getCarrier()));
            }

            // 2. Add optional filter for the start date
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("invoiceDate"), filter.getStartDate()));
            }

            // 3. Add optional filter for the end date
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("invoiceDate"), filter.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}