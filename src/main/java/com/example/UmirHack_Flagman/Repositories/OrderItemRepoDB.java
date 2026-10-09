package com.example.UmirHack_Flagman.Repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.UmirHack_Flagman.Entity.OrderItemEntity;

public interface OrderItemRepoDB extends JpaRepository<OrderItemEntity, UUID> {

    @Query("""
            select i.order.id as id, count(i) as cnt
            from OrderItemEntity i
            where i.order.id in :ids
            group by i.order.id""")
    List<IdCount> countByOrderIds(Collection<UUID> ids);
}
