package com.apexvision.optimizer.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@Slf4j
public class GraphHopperConfig {

    @Value("${graphhopper.osm-path:colombia-260204.osm.pbf}")
    private String osmFilePath;

    @Value("${graphhopper.graph-location:graph-cache}")
    private String graphLocation;

    @Bean
    public GraphHopper graphHopper() {
        log.info("Initializing GraphHopper with map: {}", osmFilePath);

        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFilePath);
        hopper.setGraphHopperLocation(graphLocation);
        hopper.setEncodedValuesString("car_average_speed");

        // Define profile: Car profile for standard logistics using CustomModel
        // Version 11.0 requires CustomModel for flexible routing
        com.graphhopper.util.CustomModel customModel = new com.graphhopper.util.CustomModel();
        customModel.addToSpeed(com.graphhopper.json.Statement.If("true", com.graphhopper.json.Statement.Op.LIMIT,
                "car_average_speed"));

        Profile profile = new Profile("car").setCustomModel(customModel);
        hopper.setProfiles(Collections.singletonList(profile));

        // Enable caching (Speed mode)
        hopper.getCHPreparationHandler().setCHProfiles(new com.graphhopper.config.CHProfile("car"));

        // Load the graph (this might take some time on first run)
        hopper.importOrLoad();

        log.info("GraphHopper initialized successfully.");
        return hopper;
    }
}
