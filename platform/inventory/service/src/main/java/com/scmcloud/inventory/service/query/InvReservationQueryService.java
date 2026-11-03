package com.scmcloud.inventory.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvReservationQueryService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RESERVATION_KEY_PREFIX = "inventory:reservation:";

    @Slave
    public boolean checkReservationExists(String businessKey) {
        String reservationKey = buildReservationKey(businessKey);
        return Boolean.TRUE.equals(redisTemplate.hasKey(reservationKey));
    }

    @Slave
    public Integer getReservedQuantity(String businessKey) {
        String reservationKey = buildReservationKey(businessKey);
        Object quantity = redisTemplate.opsForHash().get(reservationKey, "quantity");
        return quantity != null ? (Integer) quantity : null;
    }

    private String buildReservationKey(String businessKey) {
        return RESERVATION_KEY_PREFIX + businessKey;
    }
}
