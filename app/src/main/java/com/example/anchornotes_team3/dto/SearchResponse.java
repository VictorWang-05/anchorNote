package com.example.anchornotes_team3.dto;

import java.util.List;

/**
 * Response DTO for search endpoint
 * Backend returns: { total, items: [NoteResponse...] }
 */
public class SearchResponse {
    private int total;
    private List<NoteResponse> items;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<NoteResponse> getItems() {
        return items;
    }

    public void setItems(List<NoteResponse> items) {
        this.items = items;
    }
}
