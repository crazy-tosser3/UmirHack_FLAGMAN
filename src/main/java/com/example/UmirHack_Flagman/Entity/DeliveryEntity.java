package com.example.UmirHack_Flagman.Entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "deliveries")
@Setter
@Getter
public class DeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String deliveryNumber;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status = DeliveryStatus.ACCEPTED;

    protected DeliveryEntity() {
    }

    public DeliveryEntity(String deliveryNumber, Instant receivedAt) {
        this.deliveryNumber = deliveryNumber;
        this.receivedAt = receivedAt;
    }
}
