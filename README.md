# Apex Vision - Route Optimization Microservice

## General Description

This microservice serves as the **Mathematical Brain** of the Apex Vision logistics platform. Its primary responsibility is to receive an unordered list of geographic coordinates and return the most efficient delivery route using advanced Vehicle Routing Problem (VRP) solving algorithms.

The service is **stateless** and designed to integrate with the .NET Orchestrator through a REST API.

## Technology Stack

- **Language**: Java 17 (LTS)
- **Framework**: Spring Boot 3.4.12
- **Dependency Manager**: Maven
- **Optimization Engine**: Jsprit 1.9.0-beta.2
- **Routing Engine**: GraphHopper 10.2
- **Map Data**: OpenStreetMap (OSM) - Colombia
- **API Documentation**: OpenAPI / Swagger UI
- **Containerization**: Docker (Multi-Stage Build with Alpine Runtime)

## Architecture and Algorithms

This service models the routing problem using advanced combinatorial optimization techniques and implements robust software architecture patterns for high availability.

### 1. Mathematical Model

- **Problem Type**: Vehicle Routing Problem (VRP)
- **Nodes (Vertices)**: Each delivery location is a node in a complete graph
- **Edges**: Real-world road distances calculated using OpenStreetMap data
- **Objective**: Minimize total travel distance while visiting all nodes

### 2. Optimization Strategy (Strategy Pattern)

The system uses the **Strategy Pattern** to decouple optimization logic from business services.

**Current Implementation:**

- **Primary Algorithm**: Jsprit (Vehicle Routing Problem Solver)
- **Distance Calculation**: GraphHopper (real road network topology)
- **Complexity**: Internal Jsprit optimization (metaheuristics)
- **Configuration**: Single vehicle, finite fleet, no depot return

**Key Features:**
- Considers actual road networks (not straight-line distances)
- Respects real-world routing constraints
- Optimizes for delivery scenarios

### 3. Multi-Layer Resilience Pattern

To ensure **High Availability (HA)**, the service implements a three-layer resilience strategy:

#### Layer 1: Jsprit Optimization
- Primary optimization using Jsprit + GraphHopper
- Best quality solution (considers real roads)

#### Layer 2: Fallback to Original Order
- If Jsprit fails (e.g., memory overflow, calculation error)
- Returns the original location list with sequence numbers assigned
- Guarantees the driver always receives a route

#### Layer 3: Haversine Distance Fallback
- If GraphHopper fails for distance calculation
- Uses mathematical Haversine formula (straight-line distance)
- Ensures total distance is always calculated

**Result:** The service **never returns a 500 error** for routing requests. It gracefully degrades through fallback layers.

### 4. Distance Calculation

#### Primary: GraphHopper (Topological)
- Uses OpenStreetMap road network data
- Calculates actual driving distances
- Considers road topology, one-way streets, etc.

#### Fallback: Haversine Formula (Mathematical)
- Great-circle distance on Earth's surface
- Spherical trigonometry (not Euclidean)
- Used only when GraphHopper fails

**Formula:**
```
a = sin²(Δφ/2) + cos(φ₁) · cos(φ₂) · sin²(Δλ/2)
c = 2 · atan2(√a, √(1-a))
d = R · c
```
Where R = 6371 km (Earth's radius)

### 5. Map Data Requirements

**OSM File:**
- Source: [Geofabrik - Colombia](https://download.geofabrik.de/south-america/colombia.html)
- Size: ~200 MB (compressed PBF format)
- Update Frequency: Monthly

**Graph Cache:**
- Generated on first startup
- Size: ~5-10 GB
- Initialization Time: ~10-15 minutes (first run only)
- Subsequent Starts: ~30 seconds (cache reused)

**Storage Path:**
```
/app/osm-data/
├── colombia-latest.osm.pbf  (200 MB)
└── graph-cache/              (5-10 GB)
```

## API Reference

### Optimize Route

Orders a list of stops to minimize total distance traveled.

**Endpoint**: `POST /api/v1/optimize`

**Content-Type**: `application/json`

#### Request Body (Example)

```json
{
  "fleetId": "truck-north-01",
  "locations": [
    {
      "id": 101,
      "latitude": 4.59805, 
      "longitude": -74.07583 
    },
    {
      "id": 102,
      "latitude": 4.67678, 
      "longitude": -74.04823
    },
    {
      "id": 103,
      "latitude": 4.61539, 
      "longitude": -74.06915
    }
  ]
}
```

**Note:** The first element (index 0) is assumed to be the starting point (depot/warehouse).

#### Response Body (Example)

```json
{
  "totalDistanceKm": 9.27,
  "optimizedOrder": [
    {
      "id": 101,
      "latitude": 4.59805,
      "longitude": -74.07583,
      "sequenceNumber": 0
    },
    {
      "id": 103,
      "latitude": 4.61539,
      "longitude": -74.06915,
      "sequenceNumber": 1
    },
    {
      "id": 102,
      "latitude": 4.67678,
      "longitude": -74.04823,
      "sequenceNumber": 2
    }
  ]
}
```

#### Status Codes

- **200 OK**: Calculation successful
- **400 Bad Request**: Invalid data (Latitude > 90, Longitude > 180, empty list, etc.)

### Input Validation

The API validates:
- Latitude: -90 to 90
- Longitude: -180 to 180
- List must not be empty
- IDs must not be null

## Deployment and Installation

### Local Execution (Development)

```bash
# 1. Ensure OSM file is in place
mkdir -p osm-data
# Download Colombia map (or mount volume if using Docker)

# 2. Build project
./mvnw clean package

# 3. Run application
java -jar target/route-optimizer-0.0.1-SNAPSHOT.jar
```

**First Run:** Wait ~10-15 minutes for GraphHopper to process the OSM file.

### Docker Execution (Production)

The project uses a Multi-Stage Dockerfile. Maven compilation happens inside the container.

```bash
# 1. Build the image
docker build -t apex-vision/optimizer .

# 2. Run with volume mount for OSM data
docker run -p 8080:8080 \
  -v $(pwd)/osm-data:/app/osm-data \
  apex-vision/optimizer
```

**Environment Variables:**
```bash
# GraphHopper Configuration
GRAPHHOPPER_OSM_PATH=/app/osm-data/colombia-latest.osm.pbf
GRAPHHOPPER_GRAPH_LOCATION=/app/osm-data/graph-cache

# Logging
LOGGING_LEVEL_COM_APEXVISION_OPTIMIZER=INFO
```

### Interactive Documentation (Swagger)

Once deployed, live API documentation is available at:

**URL**: http://localhost:8080/swagger-ui.html

## Project Structure

```
com.apexvision.optimizer
├── controller       # REST endpoints (RouteController)
├── service          # Business logic and orchestration (RouteService)
│   └── graphhopper  # GraphHopper integration (GraphHopperService)
├── strategy         # Optimization algorithm implementations
│   └── impl         # JspritStrategyImpl (VRP solver)
├── config           # Spring configuration (GraphHopperConfig)
├── dtos             # Data transfer objects and validations
└── advice           # Global exception handling
```

## Performance Characteristics

- **Optimization Time**: 100-500ms for 10-50 locations (depends on map complexity)
- **Memory**: ~2-4 GB RAM (depending on cache size)
- **Scalability**: Stateless design allows horizontal scaling
- **Accuracy**: Real road distances (GraphHopper) vs. straight-line (Haversine fallback)

## Troubleshooting

### First Startup Takes Too Long
- **Expected**: 10-15 minutes on first run (GraphHopper processes OSM file)
- **Check logs**: `GraphHopper initialized successfully`

### Error: "Cannot find OSM file"
- **Solution**: Ensure `colombia-latest.osm.pbf` is in `/app/osm-data/`
- **Download**: https://download.geofabrik.de/south-america/colombia.html

### GraphHopper Returns Errors
- **Solution**: Clear graph cache and restart
```bash
rm -rf osm-data/graph-cache
# Restart application
```

## License

Internal use - Apex Vision Platform