package com.example.UmirHack_Flagman.Repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.UmirHack_Flagman.Entity.DeliveryEntity;

public interface DeliveryRepoDB extends JpaRepository<DeliveryEntity, UUID>, JpaSpecificationExecutor<DeliveryEntity> {

    boolean existsByDeliveryNumber(String deliveryNumber);
}
