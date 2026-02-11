# Docker Deployment Guide

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Shared Volume                            │
│  osm-data/                                                  │
│  ├── colombia-latest.osm.pbf  (200 MB)                     │
│  └── graph-cache/              (5-10 GB)                    │
└─────────────────────────────────────────────────────────────┘
                           ↑
                           │
                      ┌─────────┐
                      │ Optimizer│ (Spring Boot App)
                      └─────────┘
                           ↓
                       Port 8080
```

## First-Time Setup

### 1. Download Colombia Map

Before starting the application, download the OpenStreetMap file:

```bash
# Create data directory
mkdir -p osm-data

# Download Colombia OSM file (~200 MB)
cd osm-data
wget https://download.geofabrik.de/south-america/colombia-latest.osm.pbf

# Verify download
ls -lh colombia-latest.osm.pbf
```

**Alternative:** Use a Docker service to automate the download (see Automation section below).

### 2. Build Docker Image

```bash
# Build the image (Maven compiles code + builds runtime image)
docker build -t apex-vision/optimizer .
```

### 3. Run the Container

```bash
# Run with volume mount for OSM data
docker run -d \
  --name route-optimizer \
  -p 8080:8080 \
  -v $(pwd)/osm-data:/app/osm-data \
  apex-vision/optimizer
```

### 4. Wait for GraphHopper Initialization

**First time:** ~10-15 minutes (processes 200 MB OSM and generates 5-10 GB cache).

Monitor progress:
```bash
# View logs
docker logs -f route-optimizer

# Wait for this message:
# "GraphHopper initialized successfully"
```

### 5. Test the Endpoint

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
http://localhost:8080/swagger-ui.html

# Test route optimization
curl -X POST http://localhost:8080/api/v1/optimize \
  -H "Content-Type: application/json" \
  -d '{
    "fleetId": "test-001",
    "locations": [
      {"id": 1, "latitude": 4.598120, "longitude": -74.076037},
      {"id": 2, "latitude": 4.601968, "longitude": -74.072089}
    ]
  }'
```

---

## Monthly Map Updates

OSM data is updated regularly. To keep your routing accurate, update the map monthly.

### Option A: Manual Update

```bash
# 1. Stop the container
docker stop route-optimizer

# 2. Download new map (overwrites old one)
cd osm-data
rm colombia-latest.osm.pbf
wget https://download.geofabrik.de/south-america/colombia-latest.osm.pbf

# 3. Clear graph cache (forces rebuild)
rm -rf graph-cache

# 4. Restart container
docker start route-optimizer
```

### Option B: Automated Update (Cron)

Create a script `update_map.sh`:

```bash
#!/bin/bash
cd /path/to/your/project

# Download new map
wget -O osm-data/colombia-latest.osm.pbf \
  https://download.geofabrik.de/south-america/colombia-latest.osm.pbf

# Clear cache
rm -rf osm-data/graph-cache

# Restart container
docker restart route-optimizer

echo "Map updated: $(date)" >> /var/log/osm-updates.log
```

Schedule in crontab:
```bash
# Run on the 1st of each month at 3 AM
0 3 1 * * /path/to/your/project/update_map.sh
```

---

## Useful Commands

```bash
# View container status
docker ps

# View logs
docker logs -f route-optimizer

# View resource consumption
docker stats route-optimizer

# Restart container
docker restart route-optimizer

# Stop container
docker stop route-optimizer

# Remove container (keeps volume)
docker rm route-optimizer

# Remove everything (CAUTION: deletes volume)
docker rm -f route-optimizer
docker volume rm osm-data
```

---

## Production Configuration

### Environment Variables

Configure via `docker run` or Docker Compose `.env` file:

```env
# GraphHopper
GRAPHHOPPER_OSM_PATH=/app/osm-data/colombia-latest.osm.pbf
GRAPHHOPPER_GRAPH_LOCATION=/app/osm-data/graph-cache

# Spring Boot
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_COM_APEXVISION_OPTIMIZER=INFO

# JVM (adjust based on server RAM)
JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
```

**Example with environment variables:**

```bash
docker run -d \
  --name route-optimizer \
  -p 8080:8080 \
  -v $(pwd)/osm-data:/app/osm-data \
  -e JAVA_OPTS="-Xms2g -Xmx4g" \
  -e LOGGING_LEVEL_COM_APEXVISION_OPTIMIZER=INFO \
  apex-vision/optimizer
```

### Resource Requirements

- **Disk Space:** ~15-20 GB (OSM file + graph cache + logs)
- **Memory:** 2-4 GB RAM minimum
- **CPU:** 2+ cores recommended (for first-time graph processing)

### Monitoring

Spring Boot Actuator endpoints are available:

```bash
# Health
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

For production monitoring, integrate with **Prometheus** or **Grafana**.

---

## Troubleshooting

### Error: "Encoded values missing"
- **Cause:** Corrupted cache or incompatible GraphHopper version
- **Solution:**
  ```bash
  docker stop route-optimizer
  rm -rf osm-data/graph-cache
  docker start route-optimizer
  ```

### Slow First Load
- **Normal:** GraphHopper takes 10-15 minutes to process Colombia OSM file the first time
- **Verify:** `docker logs route-optimizer | grep "GraphHopper"`
- **Wait for:** "GraphHopper initialized successfully"

### Container Crashes on Startup
- **Check logs:** `docker logs route-optimizer`
- **Common causes:**
  - Missing OSM file
  - Insufficient memory
  - Corrupted graph cache

---

## Docker Compose (Optional)

For easier management, use Docker Compose:

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  route-optimizer:
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./osm-data:/app/osm-data
    environment:
      JAVA_OPTS: "-Xms2g -Xmx4g"
      LOGGING_LEVEL_COM_APEXVISION_OPTIMIZER: INFO
    restart: unless-stopped
```

**Commands:**
```bash
# Start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

---

## Important Notes

⚠️ **Disk Space:** Ensure 15-20 GB free (OSM + cache + logs)  
⚠️ **Memory:** Minimum 2 GB RAM, 4 GB recommended  
⚠️ **First Load:** Be patient. OSM processing is CPU-intensive  
✅ **Subsequent Starts:** Only take ~30 seconds (cache is reused)
