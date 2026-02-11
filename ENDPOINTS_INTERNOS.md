# Internal Endpoints

Services exposed solely for internal consumption by the orchestrator or authorized clients.

## General Details
*   **Protocol:** HTTP/1.1
*   **Format:** JSON (`application/json`)
*   **Base Path:** Configurable in `application.properties` (e.g., `/api/v1` or similar).
*   **Default Port:** `8080`

---

## Endpoint List

### 1. Optimize Route
Calculates the optimal delivery order based on geographical coordinates.

*   **Method:** `POST`
*   **Path:** `{base-path}/optimize`
*   **Controller:** `RouteController`

#### Request Body
Must include the fleet ID and a list of locations with Latitude/Longitude. `sequenceNumber` can be null in the request.

```json
{
  "fleetId": "FLEET-12345",
  "locations": [
    {
      "id": 101,
      "latitude": 4.6097,
      "longitude": -74.0817,
      "sequenceNumber": null
    },
    {
      "id": 102,
      "latitude": 4.6297,
      "longitude": -74.0917,
      "sequenceNumber": null
    }
  ]
}
```

#### Response Body (200 OK)
Returns the same list reordered with assigned `sequenceNumber`s and the total calculated distance.

```json
{
  "totalDistanceKm": 5.42,
  "optimizedOrder": [
    {
      "id": 101,
      "latitude": 4.6097,
      "longitude": -74.0817,
      "sequenceNumber": 0
    },
    {
      "id": 102,
      "latitude": 4.6297,
      "longitude": -74.0917,
      "sequenceNumber": 1
    }
  ]
}
```

#### Status Codes
*   `200 OK`: Calculation successful.
*   `400 Bad Request`: If lat/long are invalid or the list is empty.
