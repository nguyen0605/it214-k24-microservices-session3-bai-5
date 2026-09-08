package com.cinema.bookingservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RefreshScope
public class BookingProperties {

    @Value("${booking.seat-hold.timeout-seconds:300}")
    private int seatHoldTimeoutSeconds;

    @Value("${feature.instant-discount.enabled:false}")
    private boolean instantDiscountEnabled;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/cinemax_booking_db}")
    private String datasourceUrl;

    public Map<String, Object> getConfigDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("seatHoldTimeoutSeconds", seatHoldTimeoutSeconds);
        map.put("instantDiscountEnabled", instantDiscountEnabled);
        map.put("datasourceUrl", datasourceUrl);
        return map;
    }
}
