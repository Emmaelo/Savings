package com.emmanuelandsamuel.savings_project.utilities;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import com.emmanuelandsamuel.savings_project.dtos.requests.GroupSearchRequest;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;

public class GroupSpecification {
    public static Specification<Group> searchableGroups(
            GroupSearchRequest request) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.equal(
                            root.get("groupStatus"),
                            GroupStatus.INACTIVE));

            predicates.add(
                    cb.lessThan(
                         root.get("currentMemberCount"),
                            root.get("memberCount")));

            if (request.getGuaranteeRequired() != null) {
                predicates.add(
                        cb.equal(
                                root.get("guaranteeRequired"),
                                request.getGuaranteeRequired()));
            }

            if (request.getGroupSavingsType() != null) {
                predicates.add(
                        cb.equal(
                                root.get("groupSavingsType"),
                                request.getGroupSavingsType()));
            }

            if (request.getMinAmount() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("amountToSave"),
                                request.getMinAmount()));
            }

            if (request.getMaxAmount() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                root.get("amountToSave"),
                                request.getMaxAmount()));
            }

            return cb.and(
                    predicates.toArray(new Predicate[0]));
        };
    }

}
