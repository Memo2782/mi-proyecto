package com.ejemplo.controller;

import com.ejemplo.model.Order;
import com.ejemplo.repository.OrderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
    private static final Logger logger = LogManager.getLogger(OrderApiController.class);
    private final OrderRepository repository;

    public OrderApiController(OrderRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{orderId}/status")
    public Mono<ResponseEntity<String>> getOrderStatus(@PathVariable String orderId) {
        logger.info("Consulta REST Webflux para el estado de la orden: {}", orderId);
        return repository.findByOrderId(orderId)
                .map(order -> ResponseEntity.ok(order.getStatus()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/count-by-date")
    public Mono<ResponseEntity<Long>> getOrdersCountByDate(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        logger.info("Consulta REST Webflux para volumen de órdenes entre {} y {}", start, end);
        return repository.findByTsBetween(start, end)
                .count()
                .map(ResponseEntity::ok);
    }
}
