# Java Microservice: Route Optimizer

This microservice acts as the intelligent core of our logistics platform, dedicated to solving Vehicle Routing Problems (VRP). It implements the **Jsprit** optimization algorithm with **GraphHopper** road distance calculation to compute efficient delivery routes using real-world map data.

Here you will find detailed technical specifications regarding the optimization logic and internal APIs available for integration.

## Table of Contents

- [**Internal Endpoints**](./ENDPOINTS_INTERNOS.md)  
  *Technical reference for the exposed HTTP services and API contracts.*
  
- [**Main README**](./README.md)  
  *Complete architecture documentation, deployment instructions, and technical overview.*
  
- [**Docker Deployment**](./README_DOCKER.md)  
  *Container deployment guide with OSM map setup and configuration.*

## Quick Links

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`
- **OSM Data Source**: [Geofabrik - Colombia](https://download.geofabrik.de/south-america/colombia.html)

## Technology Stack

- Jsprit 1.9.0-beta.2 (VRP Solver)
- GraphHopper 10.2 (Road Distance)
- Spring Boot 3.4.12
- OpenStreetMap Colombia (~200 MB)