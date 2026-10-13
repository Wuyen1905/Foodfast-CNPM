package com.foodfast.repository;

import com.foodfast.entity.SubstitutionProposal;
import com.foodfast.entity.SubstitutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubstitutionProposalRepository extends JpaRepository<SubstitutionProposal, Long> {
    List<SubstitutionProposal> findByOrderIdOrderByProposedAtDesc(String orderId);
    boolean existsByOrderItemIdAndStatus(Long orderItemId, SubstitutionStatus status);
}
