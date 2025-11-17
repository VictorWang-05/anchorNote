package com.example.anchornotes_team3.geofence;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * White box tests for GeofenceManager
 * Tests the internal logic of distance calculations between coordinates using JUnit 4
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GeofenceManagerTest {

    /**
     * Test 38: Test calculating distance between coordinates
     * White box test: Verifies the internal distance calculation logic using
     * the Haversine formula or Android's Location.distanceBetween method.
     * Tests various distance scenarios to ensure accurate calculations.
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_SameLocation() {
        // Arrange: Two identical coordinates (should be 0 distance)
        double lat1 = 34.0224;  // Los Angeles
        double lon1 = -118.2851;
        double lat2 = 34.0224;  // Same location
        double lon2 = -118.2851;

        // Act: Calculate distance using Android's Location API
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        float distance = results[0];

        // Assert: Distance should be 0 (or very close to 0 due to floating point)
        assertTrue("Distance between same coordinates should be 0 or very small",
                distance < 0.1); // Within 10cm tolerance
        assertEquals("Distance should be approximately 0", 0.0f, distance, 0.1f);
    }

    /**
     * Test 38 (Extended): Test distance calculation between nearby locations
     * White box test: Verifies accurate distance calculation for short distances
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_NearbyLocations() {
        // Arrange: Two nearby coordinates in Los Angeles (approx 1km apart)
        double lat1 = 34.0522;  // Downtown LA
        double lon1 = -118.2437;
        double lat2 = 34.0622;  // About 1km north
        double lon2 = -118.2437; // Same longitude

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        float distance = results[0];

        // Assert: Distance should be approximately 1113 meters (1.11 km)
        // 1 degree of latitude ≈ 111.3 km, so 0.01 degrees ≈ 1.113 km
        assertTrue("Distance should be between 1000 and 1200 meters",
                distance >= 1000 && distance <= 1200);
        assertEquals("Distance should be approximately 1113 meters",
                1113.0f, distance, 50.0f); // 50m tolerance
    }

    /**
     * Test 38 (Extended): Test distance calculation between distant locations
     * White box test: Verifies accurate distance calculation for long distances
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_DistantLocations() {
        // Arrange: Los Angeles to New York (long distance)
        double laLat = 34.0522;
        double laLon = -118.2437;
        double nyLat = 40.7128;
        double nyLon = -74.0060;

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(laLat, laLon, nyLat, nyLon, results);
        float distance = results[0];

        // Assert: Distance should be approximately 3944 km (3,944,000 meters)
        // Actual great-circle distance is about 3,944 km
        assertTrue("Distance should be between 3.9M and 4.0M meters",
                distance >= 3_900_000 && distance <= 4_000_000);
        assertEquals("Distance should be approximately 3,944,000 meters",
                3_944_000.0f, distance, 50_000.0f); // 50km tolerance
    }

    /**
     * Test 38 (Extended): Test distance calculation across the equator
     * White box test: Verifies distance calculation works correctly
     * across different hemispheres
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_AcrossEquator() {
        // Arrange: Points on opposite sides of equator
        double northLat = 10.0;   // Northern hemisphere
        double northLon = 0.0;
        double southLat = -10.0;  // Southern hemisphere
        double southLon = 0.0;

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(northLat, northLon, southLat, southLon, results);
        float distance = results[0];

        // Assert: Distance should be approximately 2226 km (20 degrees of latitude)
        // 20 degrees * 111.3 km/degree ≈ 2226 km
        assertEquals("Distance across equator should be approximately 2,226,000 meters",
                2_226_000.0f, distance, 20_000.0f); // 20km tolerance for Earth curvature variations
    }

    /**
     * Test 38 (Extended): Test distance calculation across prime meridian
     * White box test: Verifies distance calculation works correctly
     * across the prime meridian (0° longitude)
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_AcrossPrimeMeridian() {
        // Arrange: Points on opposite sides of prime meridian, same latitude
        double lat = 51.5074; // London latitude
        double westLon = -1.0; // West of prime meridian
        double eastLon = 1.0;  // East of prime meridian

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(lat, westLon, lat, eastLon, results);
        float distance = results[0];

        // Assert: At 51.5° latitude, 1° longitude ≈ 69.5 km
        // So 2° should be approximately 139 km
        assertTrue("Distance should be between 130 and 150 km",
                distance >= 130_000 && distance <= 150_000);
        assertEquals("Distance should be approximately 139,000 meters",
                139_000.0f, distance, 10_000.0f);
    }

    /**
     * Test 38 (Extended): Test distance calculation at the poles
     * White box test: Verifies distance calculation at extreme latitudes
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_NearPoles() {
        // Arrange: Points near North Pole
        double lat1 = 89.0;  // Near North Pole
        double lon1 = 0.0;
        double lat2 = 89.0;  // Same latitude
        double lon2 = 180.0; // Opposite side of globe

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        float distance = results[0];

        // Assert: At 89° latitude, distances along longitude are very small
        // because the lines of longitude converge at the pole
        assertTrue("Distance near pole should be less than 250 km",
                distance < 250_000);
    }

    /**
     * Test 38 (Extended): Test distance calculation with negative coordinates
     * White box test: Verifies that negative latitude/longitude values
     * are handled correctly (Southern/Western hemispheres)
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_NegativeCoordinates() {
        // Arrange: Sydney, Australia (negative latitude, positive longitude)
        double sydneyLat = -33.8688;
        double sydneyLon = 151.2093;

        // Buenos Aires, Argentina (negative latitude, negative longitude)
        double buenosLat = -34.6037;
        double buenosLon = -58.3816;

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(sydneyLat, sydneyLon, buenosLat, buenosLon, results);
        float distance = results[0];

        // Assert: Distance should be approximately 11,800 km
        assertTrue("Distance should be between 11.5M and 12M meters",
                distance >= 11_500_000 && distance <= 12_000_000);
    }

    /**
     * Test 38 (Extended): Test distance calculation for geofence boundary
     * White box test: Verifies distance calculation can accurately determine
     * if a point is within/outside a geofence radius
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_GeofenceBoundary() {
        // Arrange: Geofence center and test points
        double centerLat = 34.0224;
        double centerLon = -118.2851;

        // Point just inside 100m radius (approximately 0.0009 degrees at this latitude)
        double insideLat = centerLat + 0.0008; // ~89m north
        double insideLon = centerLon;

        // Point just outside 100m radius
        double outsideLat = centerLat + 0.001; // ~111m north
        double outsideLon = centerLon;

        // Act: Calculate distances
        float[] insideResults = new float[1];
        Location.distanceBetween(centerLat, centerLon, insideLat, insideLon, insideResults);
        float insideDistance = insideResults[0];

        float[] outsideResults = new float[1];
        Location.distanceBetween(centerLat, centerLon, outsideLat, outsideLon, outsideResults);
        float outsideDistance = outsideResults[0];

        // Assert: Verify boundary detection
        float geofenceRadius = 100.0f; // 100 meters

        assertTrue("Inside point should be within geofence radius",
                insideDistance < geofenceRadius);
        assertTrue("Outside point should be beyond geofence radius",
                outsideDistance > geofenceRadius);

        // Verify approximate distances
        assertEquals("Inside distance should be approximately 89 meters",
                89.0f, insideDistance, 5.0f);
        assertEquals("Outside distance should be approximately 111 meters",
                111.0f, outsideDistance, 5.0f);
    }

    /**
     * Test 38 (Extended): Test distance calculation precision
     * White box test: Verifies that distance calculations maintain
     * sufficient precision for geofencing purposes
     */
    @Test
    public void testCalculateDistanceBetweenCoordinates_Precision() {
        // Arrange: Very close points (1 meter apart)
        double lat1 = 34.0522;
        double lon1 = -118.2437;

        // Move approximately 1 meter north
        // At this latitude, 0.000009 degrees ≈ 1 meter
        double lat2 = lat1 + 0.000009;
        double lon2 = lon1;

        // Act: Calculate distance
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        float distance = results[0];

        // Assert: Should detect sub-meter distances
        assertTrue("Should be able to measure distances less than 2 meters",
                distance < 2.0f);
        assertTrue("Distance should be approximately 1 meter",
                distance >= 0.8f && distance <= 1.2f);
    }
}
