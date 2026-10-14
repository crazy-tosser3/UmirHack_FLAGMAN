package com.example.UmirHack_Flagman.inventory;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliveryRepoDB extends JpaRepository<DeliveryEntity, UUID>, JpaSpecificationExecutor<DeliveryEntity> {

    boolean existsByDeliveryNumber(String deliveryNumber);
}
