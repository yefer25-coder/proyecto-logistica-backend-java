package com.apexvision.optimizer.strategy;

import com.apexvision.optimizer.dtos.LocationDto;
import java.util.List;


public interface OptimizationStrategy {
    List<LocationDto> calculateOptimalRoute(List<LocationDto> locations);
}
