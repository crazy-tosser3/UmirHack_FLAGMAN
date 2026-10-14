package com.example.UmirHack_Flagman.orders;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.UmirHack_Flagman.common.IdCount;

public interface OrderItemRepoDB extends JpaRepository<OrderItemEntity, UUID> {

    @Query("""
            select i.order.id as id, count(i) as cnt
            from OrderItemEntity i
            where i.order.id in :ids
            group by i.order.id""")
    List<IdCount> countByOrderIds(Collection<UUID> ids);

    @Query("""
            select d.id as dishId, d.name as dishName,
                   sum(i.quantity) as portions, count(distinct o.id) as orders
            from OrderItemEntity i join i.order o join i.dish d
            where o.createdAt >= :from and o.createdAt < :to and o.status = :status
            group by d.id, d.name
            order by sum(i.quantity) desc, d.name""")
    List<DishDayRow> salesByDish(Instant from, Instant to, OrderStatus status);
}
