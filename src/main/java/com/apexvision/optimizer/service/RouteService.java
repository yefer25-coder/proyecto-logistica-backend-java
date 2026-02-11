package com.apexvision.optimizer.service;

import com.apexvision.optimizer.dtos.LocationDto;
import com.apexvision.optimizer.dtos.RouteRequest;
import com.apexvision.optimizer.dtos.RouteResponse;
import com.apexvision.optimizer.service.graphhopper.GraphHopperService;
import com.apexvision.optimizer.strategy.OptimizationStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService {

    private final OptimizationStrategy strategy;
    private final GraphHopperService graphHopperService;

    /**
 * Optimizes delivery route and calculates total distance.
 * Falls back to original order if optimization fails.
 */
    public RouteResponse optimizeRoute(RouteRequest request) {

        List<LocationDto> originalList = request.getLocations();
        List<LocationDto> finalRoute;

        try {
            finalRoute = strategy.calculateOptimalRoute(originalList);
        } catch (Exception e) {
            log.error("Error in optimization algorithm: {}. Using original list.", e.getMessage());
            finalRoute = originalList;
            for (int i = 0; i < finalRoute.size(); i++)
                finalRoute.get(i).setSequenceNumber(i);
        }

        double totalDistance = calculateTotalDistanceSafely(finalRoute);

        return RouteResponse.builder()
                .totalDistanceKm(totalDistance)
                .optimizedOrder(finalRoute)
                .build();
    }

    // Calculate total distance with resilience fallback
    private double calculateTotalDistanceSafely(List<LocationDto> route) {
        if (route == null || route.size() < 2) {
            return 0.0;
        }

        double total = 0.0;

        for (int i = 0; i < route.size() - 1; i++) {
            LocationDto start = route.get(i);
            LocationDto end = route.get(i + 1);

            try {
                // LEVEL 1: Use GraphHopper Road Distance (Topological - Most Accurate)
                total += graphHopperService.getRoadDistance(
                        start.getLatitude(), start.getLongitude(),
                        end.getLatitude(), end.getLongitude());
            } catch (Exception e) {
                // LEVEL 2: Fallback to Haversine (Mathematical - Always Available)
                log.warn("GraphHopper failed for segment {}->{}, using Haversine fallback: {}",
                        i, i + 1, e.getMessage());
                total += calculateHaversineDistance(
                        start.getLatitude(), start.getLongitude(),
                        end.getLatitude(), end.getLongitude());
            }
        }

        return Math.round(total * 100.0) / 100.0;
    }

    /**
     * Haversine Formula for calculating straight-line distance between two
     * geographic points.
     * This is a pure mathematical fallback that never fails.
     * 
     * @return Distance in kilometers
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
