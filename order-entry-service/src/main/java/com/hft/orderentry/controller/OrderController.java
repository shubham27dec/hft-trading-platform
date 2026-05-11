package com.hft.orderentry.controller;

import com.hft.orderentry.dto.OrderRequest;
import com.hft.orderentry.dto.OrderResponse;
import com.hft.orderentry.exception.QuoteUnavailableException;
import com.hft.orderentry.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse submitOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.submitOrder(request);
    }

    @ExceptionHandler(QuoteUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleQuoteUnavailable(QuoteUnavailableException ex) {
        return Map.of("error", ex.getMessage());
    }
}
