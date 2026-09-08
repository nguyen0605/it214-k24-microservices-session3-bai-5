package com.cinema.bookingservice.controller;

import com.cinema.bookingservice.config.BookingProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingProperties bookingProperties;

    public BookingController(BookingProperties bookingProperties) {
        this.bookingProperties = bookingProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "booking-service");
        response.put("loadedConfig", bookingProperties.getConfigDetails());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/hold-seats")
    public ResponseEntity<Map<String, Object>> holdSeats(@RequestParam String movieId, @RequestParam String seatNo) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("movieId", movieId);
        response.put("seatNo", seatNo);
        response.put("message", "Seats held successfully");
        response.put("holdTimeoutSeconds", bookingProperties.getConfigDetails().get("seatHoldTimeoutSeconds"));
        return ResponseEntity.ok(response);
    }
}
