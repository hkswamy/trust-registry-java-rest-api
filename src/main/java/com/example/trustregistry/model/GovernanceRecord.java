package com.example.trustregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty; // Import this
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovernanceRecord {
    private String id;
    @JsonProperty("created_at") // Map to snake_case for JSON
    private String createdAt;
    @JsonProperty("updated_at") // Map to snake_case for JSON
    private String updatedAt;
    @JsonProperty("deleted_at") // Map to snake_case for JSON (optional, but good for consistency)
    private String deletedAt;
    private String identifier;
    private String name;
    private String status;
}
