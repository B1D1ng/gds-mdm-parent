package com.ebay.behavior.gds.mdm.common.model.search;

import com.ebay.behavior.gds.mdm.common.model.Model;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Locale.US;

@UtilityClass
public class SearchSpecification {

    public static Pageable getPageable(RelationalSearchRequest request) {
        val sort = request.getSort() != null
                ? Sort.by(request.getSort().getDirection(), request.getSort().getField())
                : Sort.unsorted();

        return PageRequest.of(request.getPageNumber(), request.getPageSize(), sort);
    }

    public static <M extends Model> Specification<M> getSpecification(RelationalSearchRequest request, boolean useSort, Class<M> entityType) {
        return (Root<M> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (val filter : CollectionUtils.emptyIfNull(request.getFilters())) {
                val field = filter.getField();
                val value = filter.getValue();

                switch (filter.getOperator()) {
                    case EXACT_MATCH:
                        predicates.add(builder.equal(root.get(field), value));
                        break;
                    case EXACT_MATCH_IGNORE_CASE:
                        predicates.add(builder.equal(builder.lower(root.get(field)), value.toLowerCase(US)));
                        break;
                    case STARTS_WITH:
                        predicates.add(builder.like(root.get(field), value + "%"));
                        break;
                    case STARTS_WITH_IGNORE_CASE:
                        predicates.add(builder.like(builder.lower(root.get(field)), value.toLowerCase(US) + "%"));
                        break;
                    case CONTAINS:
                        predicates.add(builder.like(root.get(field), "%" + value + "%"));
                        break;
                    case CONTAINS_IGNORE_CASE:
                        predicates.add(builder.like(builder.lower(root.get(field)), "%" + value.toLowerCase(US) + "%"));
                        break;
                    case GREATER_THAN:
                        predicates.add(builder.greaterThan(root.get(field), value));
                        break;
                    case GREATER_THAN_OR_EQUAL_TO:
                        predicates.add(builder.greaterThanOrEqualTo(root.get(field), value));
                        break;
                    case LESS_THAN:
                        predicates.add(builder.lessThan(root.get(field), value));
                        break;
                    case LESS_THAN_OR_EQUAL_TO:
                        predicates.add(builder.lessThanOrEqualTo(root.get(field), value));
                        break;
                    case NOT_EQUAL:
                        predicates.add(builder.notEqual(root.get(field), value));
                        break;
                    case NOT_EQUAL_IGNORE_CASE:
                        predicates.add(builder.notEqual(builder.lower(root.get(field)), value.toLowerCase(US)));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported search criterion: " + filter.getOperator());
                }
            }

            if (useSort && Objects.nonNull(request.getSort())) {
                val field = request.getSort().getField();
                val direction = request.getSort().getDirection();

                query.orderBy(Sort.Direction.ASC.equals(direction)
                        ? builder.asc(root.get(field)) : builder.desc(root.get(field)));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
