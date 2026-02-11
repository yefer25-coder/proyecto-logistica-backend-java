package com.apexvision.optimizer.dtos;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RouteResponse {
    private double totalDistanceKm;
    private List<LocationDto> optimizedOrder;
}