package com.hft.position.controller;

import com.hft.core.model.Position;
import com.hft.position.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @GetMapping("/{accountId}")
    public ResponseEntity<List<Position>> getPositions(@PathVariable String accountId) {
        return ResponseEntity.ok(positionService.getPositions(accountId));
    }
}
