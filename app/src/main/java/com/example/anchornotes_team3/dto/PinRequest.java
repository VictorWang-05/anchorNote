package com.example.anchornotes_team3.dto;

/**
 * Request DTO for pinning/unpinning a note
 */
public class PinRequest {
    private Boolean pinned;

    public PinRequest(Boolean pinned) {
        this.pinned = pinned;
    }

    public Boolean getPinned() {
        return pinned;
    }
}

