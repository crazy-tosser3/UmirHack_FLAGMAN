package com.example.UmirHack_Flagman.orders;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CustomerOrderRepoDB extends JpaRepository<CustomerOrderEntity, UUID>, JpaSpecificationExecutor<CustomerOrderEntity> {

    boolean existsByExternalOrderId(String externalOrderId);

    long countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(OrderStatus status, Instant from, Instant to);

    @Query("""
            select distinct o from CustomerOrderEntity o
            left join fetch o.items i left join fetch i.dish
            where o.id = :id""")
    Optional<CustomerOrderEntity> findWithItems(UUID id);
}
