package com.apexvision.optimizer.controller;

import com.apexvision.optimizer.dtos.RouteRequest;
import com.apexvision.optimizer.dtos.RouteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Route Optimization", description = "Vehicle Routing Problem (VRP) solver using Jsprit algorithm with real-world road distances")
public interface RouteControllerApi {

    @Operation(summary = "Optimize delivery route", description = """
            Optimizes the order of delivery locations using:
            - **Jsprit VRP Solver**: Industry-standard Vehicle Routing Problem algorithm
            - **GraphHopper**: Real road distances based on OpenStreetMap data for Colombia
            - **Multi-layer resilience**: Falls back to original order if optimization fails, uses Haversine if GraphHopper fails

            The first location in the request is treated as the depot (starting point).
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Route optimized successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data (e.g., invalid coordinates, empty location list)", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal server error (should not occur due to resilience patterns)", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/optimize")
    ResponseEntity<RouteResponse> optimize(@Valid @RequestBody RouteRequest request);
}
