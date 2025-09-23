package com.tem.be.api.service;

import com.tem.be.api.dto.InvoiceFilterDto;
import com.tem.be.api.model.Invoiceable;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A generic factory for creating JPA Specifications for any entity that implements Invoiceable.
 * This centralized class prevents code duplication for filter logic across different invoice types.
 */
public class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    /**
     * Creates a generic JPA Specification based on dynamic filter criteria.
     * This method works for any Invoiceable entity that has "department" and "invoiceDate" fields.
     *
     * @param filter The DTO containing all filter parameters.
     * @param <T>    The type of the invoice entity (e.g., FirstNetInvoice, ATTInvoice).
     * @return A composed Specification for querying the given entity type.
     */
    public static <T extends Invoiceable> Specification<T> findByCriteria(InvoiceFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by department
            if (filter.getDepartments() != null && !filter.getDepartments().isEmpty()) {
                predicates.add(root.get("department").in(filter.getDepartments()));
            }

            // Filter by date range
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("invoiceDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("invoiceDate"), filter.getEndDate()));
            }

            // Filter by a generic keyword search across all string/number fields
            createKeywordPredicate(root, criteriaBuilder, filter.getKeyword())
                    .ifPresent(predicates::add);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a broad "OR" predicate that searches the given keyword across all
     * String and Number fields of the entity using reflection.
     */
    private static <T> Optional<Predicate> createKeywordPredicate(Root<T> root, CriteriaBuilder cb, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Optional.empty();
        }

        String likePattern = "%" + keyword.toLowerCase() + "%";
        Class<?> entityClass = root.getModel().getJavaType();

        Predicate[] orPredicates = Arrays.stream(entityClass.getDeclaredFields())
                // Find all fields that are either String OR a subclass of Number.
                .filter(field -> field.getType().equals(String.class) || Number.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    if (field.getType().equals(String.class)) {
                        // For String fields, use a standard case-insensitive LIKE.
                        return cb.like(cb.lower(root.get(field.getName())), likePattern);
                    } else {
                        // For numbers, cast them to string in the DB to perform a LIKE search
                        Expression<String> numberAsString = root.get(field.getName()).as(String.class);
                        return cb.like(numberAsString, "%" + keyword + "%");
                    }
                })
                .toArray(Predicate[]::new);

        if (orPredicates.length == 0) {
            return Optional.empty();
        }

        return Optional.of(cb.or(orPredicates));
    }
}
