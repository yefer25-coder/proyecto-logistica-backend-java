package com.apexvision.optimizer.controller;

import com.apexvision.optimizer.dtos.RouteRequest;
import com.apexvision.optimizer.dtos.RouteResponse;
import com.apexvision.optimizer.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.base-path}")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RouteController implements RouteControllerApi {

    private final RouteService routeService;

    @Override
    public ResponseEntity<RouteResponse> optimize(@Valid @RequestBody RouteRequest request) {

        log.info("Optimization request received for fleet ID: {}", request.getFleetId());

        RouteResponse response = routeService.optimizeRoute(request);

        log.info("Route optimized successfully. Total distance: {} Km", response.getTotalDistanceKm());

        return ResponseEntity.ok(response);
    }
}