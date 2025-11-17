package com.example.anchornotes_team3;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.anchornotes_team3.dto.AuthRequest;
import com.example.anchornotes_team3.dto.RegisterRequest;
import com.example.anchornotes_team3.dto.CreateNoteRequest;
import com.example.anchornotes_team3.dto.UpdateNoteRequest;
import com.example.anchornotes_team3.dto.GeofenceRequest;
import com.example.anchornotes_team3.dto.SetTagsRequest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * White-box unit tests for Request DTOs
 * Tests internal logic, field storage, and JSON serialization of request objects
 */
public class RequestDtoTest {

    /**
     * Test 39: AuthRequest - Test request object with valid credentials
     * White box test: Verifies that the constructor correctly stores username and password
     * and that getters return the exact values passed during construction.
     */
    @Test
    public void authRequest_withValidCredentials_storesUsernameAndPassword() {
        // Arrange: Set up test credentials
        String username = "victor_user";
        String password = "StrongPass123";

        // Act: Create AuthRequest via constructor
        AuthRequest request = new AuthRequest(username, password);

        // Assert: Verify getters return exactly what was passed to constructor
        assertEquals("Username should match constructor argument",
                username, request.getUsername());
        assertEquals("Password should match constructor argument",
                password, request.getPassword());
    }

    /**
     * Test 40: RegisterRequest - Test registration with all required fields
     * White box test: Ensures the registration DTO correctly holds all required fields:
     * username, email, password, and fullName. Verifies that all getters return
     * the exact values set through the constructor.
     */
    @Test
    public void registerRequest_withAllFields_setsAllProperties() {
        // Arrange: Provide realistic registration values
        String username = "victor";
        String email = "victor@example.com";
        String password = "StrongPass123";
        String fullName = "Victor Leonardo Wang";

        // Act: Construct RegisterRequest with all fields
        RegisterRequest request = new RegisterRequest(username, email, password, fullName);

        // Assert: Verify all four getters return expected values
        assertEquals("Username should match constructor argument",
                username, request.getUsername());
        assertEquals("Email should match constructor argument",
                email, request.getEmail());
        assertEquals("Password should match constructor argument",
                password, request.getPassword());
        assertEquals("Full name should match constructor argument",
                fullName, request.getFullName());
    }

    /**
     * Test 41: CreateNoteRequest - Test note creation request serialization
     * White box test: Verifies that:
     * 1. The constructor sets title and text correctly
     * 2. Optional fields (pinned, tagIds, backgroundColor) can be set via setters
     * 3. When serialized with Gson, the JSON contains correct keys and values
     * This ensures the DTO correctly communicates with the backend API.
     */
    @Test
    public void createNoteRequest_serialization_containsTitleTextAndOptionalFields() {
        // Arrange: Build a CreateNoteRequest with title, text, and optional fields
        CreateNoteRequest request = new CreateNoteRequest("Trip plan", "Visit Hong Kong attractions");
        request.setPinned(true);
        request.setTagIds(Arrays.asList("tag1", "tag2"));
        request.setBackgroundColor("#FFEEAA");

        Gson gson = new Gson();

        // Act: Serialize to JSON and parse back into a map for assertions
        String json = gson.toJson(request);

        // Parse back into a generic map for easy field assertions
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> jsonMap = gson.fromJson(json, type);

        // Assert: Verify all fields are correctly serialized
        assertEquals("Title should be serialized correctly",
                "Trip plan", jsonMap.get("title"));
        assertEquals("Text should be serialized correctly",
                "Visit Hong Kong attractions", jsonMap.get("text"));
        assertEquals("Pinned should be serialized as true",
                true, jsonMap.get("pinned"));
        assertEquals("Background color should be serialized correctly",
                "#FFEEAA", jsonMap.get("backgroundColor"));

        // Verify tagIds array
        @SuppressWarnings("unchecked")
        List<?> tagIds = (List<?>) jsonMap.get("tagIds");
        assertNotNull("Tag IDs should not be null", tagIds);
        assertEquals("Tag IDs should contain 2 elements", 2, tagIds.size());
        assertTrue("Tag IDs should contain 'tag1'", tagIds.contains("tag1"));
        assertTrue("Tag IDs should contain 'tag2'", tagIds.contains("tag2"));
    }

    /**
     * Test 42: UpdateNoteRequest - Test update request with partial data
     * White box test: Demonstrates that UpdateNoteRequest can represent a partial update,
     * where some fields are set and others remain null. This matches the backend's pattern
     * where null fields mean "don't change this field". This is crucial for update operations
     * that only modify specific note properties.
     */
    @Test
    public void updateNoteRequest_partialUpdate_allowsNullForUnchangedFields() {
        // Arrange: Create UpdateNoteRequest with no-arg constructor
        UpdateNoteRequest request = new UpdateNoteRequest();

        // Act: Set only text and backgroundColor (partial update)
        request.setText("Updated note body");
        request.setBackgroundColor("#112233");

        // Assert: Verify set fields are correct
        assertEquals("Text should be set correctly",
                "Updated note body", request.getText());
        assertEquals("Background color should be set correctly",
                "#112233", request.getBackgroundColor());

        // Assert: Fields we did not set should remain null (representing "no change")
        assertNull("Title should be null when not set", request.getTitle());
        assertNull("Pinned should be null when not set", request.getPinned());
        assertNull("Tag IDs should be null when not set", request.getTagIds());

        // This demonstrates the partial update semantics: only modified fields are sent to backend
    }

    /**
     * Test 43: GeofenceRequest - Test request with valid lat/long values
     * White box test: Verifies that GeofenceRequest correctly stores valid latitude,
     * longitude, radius, and optional addressName. Tests the internal logic of
     * coordinate storage and retrieval, ensuring precision is maintained for
     * accurate geofencing calculations.
     */
    @Test
    public void geofenceRequest_withValidLatLongAndRadius_storesValues() {
        // Arrange: Choose a realistic location (Hong Kong - Central)
        Double latitude = 22.302711;
        Double longitude = 114.177216;
        Integer radius = 150;

        // Act: Construct GeofenceRequest with lat/long/radius and set addressName
        GeofenceRequest request = new GeofenceRequest(latitude, longitude, radius, "Hong Kong - Central");

        // Assert: Verify all values are stored correctly with proper precision
        assertEquals("Latitude should match constructor argument",
                latitude, request.getLatitude(), 0.000001);
        assertEquals("Longitude should match constructor argument",
                longitude, request.getLongitude(), 0.000001);
        assertEquals("Radius should match constructor argument",
                radius, request.getRadius());
        assertEquals("Address name should match the set value",
                "Hong Kong - Central", request.getAddressName());

        // Test alternate constructor without addressName
        GeofenceRequest requestNoAddress = new GeofenceRequest(latitude, longitude, radius);
        assertNull("Address name should be null when not provided in constructor",
                requestNoAddress.getAddressName());
    }

    /**
     * Test 44: SetTagsRequest - Test tags array serialization
     * White box test: Verifies that SetTagsRequest:
     * 1. Correctly stores the list of tag IDs through constructor and setter
     * 2. Serializes to JSON as an array under the "tagIds" key
     * This ensures proper communication with the backend API for tag management.
     */
    @Test
    public void setTagsRequest_serialization_containsTagIdsArray() {
        // Arrange: Create a list of tag IDs
        List<String> tagIds = Arrays.asList("1", "2", "3");
        SetTagsRequest request = new SetTagsRequest(tagIds);

        Gson gson = new Gson();

        // Act: Serialize to JSON and parse back for assertion
        String json = gson.toJson(request);

        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> jsonMap = gson.fromJson(json, type);

        // Assert: Verify JSON structure and content
        assertTrue("JSON should contain tagIds key",
                jsonMap.containsKey("tagIds"));

        @SuppressWarnings("unchecked")
        List<?> jsonTags = (List<?>) jsonMap.get("tagIds");
        assertNotNull("Tag IDs should not be null", jsonTags);
        assertEquals("Tag IDs should contain 3 elements", 3, jsonTags.size());
        assertTrue("Tag IDs should contain '1'", jsonTags.contains("1"));
        assertTrue("Tag IDs should contain '2'", jsonTags.contains("2"));
        assertTrue("Tag IDs should contain '3'", jsonTags.contains("3"));

        // Test setter method
        List<String> newTagIds = Arrays.asList("4", "5");
        request.setTagIds(newTagIds);
        assertEquals("Setter should update tag IDs",
                newTagIds, request.getTagIds());

        // Test no-arg constructor
        SetTagsRequest emptyRequest = new SetTagsRequest();
        assertNull("Tag IDs should be null for no-arg constructor",
                emptyRequest.getTagIds());
    }
}

