package com.tem.be.api.service;

import com.tem.be.api.dto.InventoryFilterDto;
import com.tem.be.api.model.Inventoryable;
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
 * A generic factory for creating JPA Specifications for any entity that implements Inventoryable.
 */
public class InventorySpecifications {

    private InventorySpecifications() {
    }

    /**
     * Creates a generic JPA Specification based on dynamic filter criteria.
     * This method works for any entity that has "department" and "lastUpdatedDate" fields.
     *
     * @param filter The DTO containing all filter parameters.
     * @param <T>    The type of the inventory entity (e.g., FirstNetInventory, ATTInventory).
     * @return A composed Specification for querying.
     */
    public static <T extends Inventoryable> Specification<T> findByCriteria(InventoryFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by department
            if (filter.getDepartments() != null && !filter.getDepartments().isEmpty()) {
                predicates.add(root.get("department").in(filter.getDepartments()));
            }

            // Filter by date range (using 'lastUpdatedDate')
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("lastUpdatedDate"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("lastUpdatedDate"), filter.getEndDate()));
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
                .filter(field -> field.getType().equals(String.class) || Number.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    if (field.getType().equals(String.class)) {
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
