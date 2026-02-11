package com.apexvision.optimizer.service.graphhopper;

import com.apexvision.optimizer.exception.OptimizationCalculationException;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.shapes.GHPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphHopperService {

    private final GraphHopper graphHopper;

    public double getRoadDistance(double lat1, double lon1, double lat2, double lon2) {
        GHRequest request = new GHRequest(
                new GHPoint(lat1, lon1),
                new GHPoint(lat2, lon2))
                .setProfile("car")
                .setLocale(Locale.US);

        GHResponse response = graphHopper.route(request);

        if (response.hasErrors()) {
            throw new OptimizationCalculationException("Cannot calculate road distance: " + response.getErrors().toString(),
                    null);
        }

        // Return distance in Kilometers (GraphHopper returns Meters)
        return response.getBest().getDistance() / 1000.0;
    }
}
