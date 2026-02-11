package com.apexvision.optimizer.strategy.impl;

import com.apexvision.optimizer.dtos.LocationDto;
import com.apexvision.optimizer.strategy.OptimizationStrategy;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.AbstractForwardVehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.driver.Driver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.*;

@Component("JspritOptimizationStrategy")
@RequiredArgsConstructor
@Slf4j
public class JspritStrategyImpl implements OptimizationStrategy {

    private final GraphHopper graphHopper;

    @PostConstruct
    public void init() {
        log.debug("Jsprit strategy initialized and ready");
    }

    @Override
    public List<LocationDto> calculateOptimalRoute(List<LocationDto> locations) {
        if (locations == null || locations.isEmpty())
            return new ArrayList<>();
        if (locations.size() == 1)
            return locations;

        log.info("Starting Jsprit optimization for {} locations", locations.size());

        try {
            // Separate start node from services
            log.info("Separating start node from services...");
            LocationDto startNode = locations.get(0);
            List<LocationDto> services = locations.subList(1, locations.size());

            // Build VRP problem
            VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
            vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);

            // Define vehicle
            VehicleType type = VehicleTypeImpl.Builder.newInstance("delivery_vehicle_type").build();
            VehicleImpl vehicle = VehicleImpl.Builder.newInstance("my_vehicle")
                    .setStartLocation(Location.newInstance(startNode.getId().toString()))
                    .setType(type)
                    .setReturnToDepot(false)
                    .build();
            vrpBuilder.addVehicle(vehicle);
            log.info("Vehicle added: {}", vehicle.getId());

            // Add service jobs
            for (LocationDto loc : services) {
                vrpBuilder.addJob(Service.Builder.newInstance(loc.getId().toString())
                        .setLocation(Location.newInstance(loc.getId().toString()))
                        .build());
            }

            // Set routing costs using GraphHopper
            vrpBuilder.setRoutingCost(new GraphHopperCostMatrix(locations, graphHopper));

            VehicleRoutingProblem problem = vrpBuilder.build();
            log.info("VRP Problem built. Jobs: {}", problem.getJobs().values().size());

            // Run optimization algorithm
            VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);
            log.info("Algorithm created. Searching solutions...");
            Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
            log.info("Search finished. Solutions found: {}", solutions.size());
            VehicleRoutingProblemSolution bestSolution = solutions.iterator().next();

            // Map solution to DTOs
            List<LocationDto> result = mapSolutionToDtos(bestSolution, startNode, services);
            log.info("Solution mapped. Total points: {}", result.size());
            return result;

        } catch (Exception e) {
            log.error("JSPRIT CRITICAL ERROR: ", e);
            throw e;
        }
    }

    private List<LocationDto> mapSolutionToDtos(VehicleRoutingProblemSolution solution, LocationDto startNode,
            List<LocationDto> originalServices) {
        List<LocationDto> optimizedRoute = new ArrayList<>();

        startNode.setSequenceNumber(0);
        optimizedRoute.add(startNode);

        for (VehicleRoute route : solution.getRoutes()) {
            for (TourActivity activity : route.getActivities()) {
                String locationId = activity.getLocation().getId();

                originalServices.stream()
                        .filter(dto -> dto.getId().toString().equals(locationId))
                        .findFirst()
                        .ifPresent(optimizedRoute::add);
            }
        }

        for (int i = 0; i < optimizedRoute.size(); i++) {
            optimizedRoute.get(i).setSequenceNumber(i);
        }

        return optimizedRoute;
    }

    /**
     * Internal cost matrix implementation using GraphHopper for real-world routing.
     */
    private static class GraphHopperCostMatrix extends AbstractForwardVehicleRoutingTransportCosts {
        private final Map<String, GHPoint> pointMap = new HashMap<>();
        private final GraphHopper hopper;

        private final Map<String, Double> distanceCache = new HashMap<>();
        private final Map<String, Double> timeCache = new HashMap<>();

        public GraphHopperCostMatrix(List<LocationDto> locations, GraphHopper hopper) {
            this.hopper = hopper;
            for (LocationDto loc : locations) {
                pointMap.put(loc.getId().toString(), new GHPoint(loc.getLatitude(), loc.getLongitude()));
            }
        }

        @Override
        public double getTransportCost(Location from, Location to, double departureTime,
                Driver driver, Vehicle vehicle) {
            return getDistance(from, to, departureTime, vehicle);
        }

        @Override
        public double getTransportTime(Location from, Location to, double departureTime,
                Driver driver, Vehicle vehicle) {
            String key = from.getId() + "->" + to.getId();

            if (from.getId().equals(to.getId()))
                return 0.0;

            if (timeCache.containsKey(key))
                return timeCache.get(key);

            GHResponse rsp = calculateRoute(from, to);

            if (rsp.hasErrors()) {
                return Double.MAX_VALUE;
            }

            if (rsp.getAll().isEmpty()) {
                return Double.MAX_VALUE;
            }

            double time = rsp.getBest().getTime() / 1000.0;
            timeCache.put(key, time);
            return time;
        }

        public double getDistance(Location from, Location to, double departureTime,
                Vehicle vehicle) {
            String key = from.getId() + "->" + to.getId();

            if (from.getId().equals(to.getId()))
                return 0.0;

            if (distanceCache.containsKey(key))
                return distanceCache.get(key);

            GHResponse rsp = calculateRoute(from, to);

            if (rsp.hasErrors()) {
                // Log errors if necessary, but return MAX_VALUE to prompt Jsprit to find an
                // alternative route.
                return Double.MAX_VALUE;
            }

            if (rsp.getAll().isEmpty()) {
                return Double.MAX_VALUE;
            }

            double dist = rsp.getBest().getDistance();
            distanceCache.put(key, dist);
            return dist;
        }

        private GHResponse calculateRoute(Location from, Location to) {
            if (from.getId().equals(to.getId()))
                return new GHResponse();

            GHPoint start = pointMap.get(from.getId());
            GHPoint end = pointMap.get(to.getId());

            GHRequest req = new GHRequest(start, end)
                    .setProfile("car")
                    .setLocale(Locale.US);

            return hopper.route(req);
        }
    }
}