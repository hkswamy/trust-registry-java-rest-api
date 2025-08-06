package com.example.trustregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty; // Import this
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustRecord {
    private String id;
    @JsonProperty("created_at") // Map to snake_case for JSON
    private String createdAt;
    @JsonProperty("updated_at") // Map to snake_case for JSON
    private String updatedAt;
    @JsonProperty("deleted_at") // Map to snake_case for JSON (optional)
    private String deletedAt;
    private String identifier;
    @JsonProperty("entity_type")
    private String entityType;
    @JsonProperty("credential_type")
    private String credentialType;
    @JsonProperty("governance_framework_uri")
    private String governanceFrameworkURI;
    @JsonProperty("did_document")
    private String didDocument;
    @JsonProperty("valid_from_dt")
    private String validFromDt;
    @JsonProperty("valid_until_dt")
    private String validUntilDt;
    private String status;
    @JsonProperty("status_detail")
    private String statusDetail;
}
