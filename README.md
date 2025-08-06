I will update your `README.md` file to include sections on how to use Locust for performance testing and how to set up and use the Streamlit UI for API testing.

-----

# trust-registry-java-rest-api

This project provides a Spring Boot RESTful API that acts as a client for a Hyperledger Fabric blockchain network, specifically interacting with a "Trust Registry" chaincode. It exposes endpoints to manage governance and trust records on the distributed ledger.

## Table of Contents

- [Project Overview](https://www.google.com/search?q=%23project-overview)
- [Prerequisites](https://www.google.com/search?q=%23prerequisites)
- [Setup and Running Instructions](https://www.google.com/search?q=%23setup-and-running-instructions)
    - [1. Set Up Hyperledger Fabric Network](https://www.google.com/search?q=%231-set-up-hyperledger-fabric-network)
    - [2. Deploy the Trust Registry Chaincode](https://www.google.com/search?q=%232-deploy-the-trust-registry-chaincode)
    - [3. Configure the Spring Boot Application](https://www.google.com/search?q=%233-configure-the-spring-boot-application)
    - [4. Build and Run the Spring Boot Application](https://www.google.com/search?q=%234-build-and-run-the-spring-boot-application)
- [API Usage (with Postman)](https://www.google.com/search?q=%23api-usage-with-postman)
    - [Importing Postman Collection](https://www.google.com/search?q=%23importing-postman-collection)
    - [Using Pre-request Scripts for IDs and Timestamps](https://www.google.com/search?q=%23using-pre-request-scripts-for-ids-and-timestamps)
    - [Endpoints Overview](https://www.google.com/search?q=%23endpoints-overview)
- [Performance Testing with Locust](https://www.google.com/search?q=%23performance-testing-with-locust)
    - [Locust Prerequisites](https://www.google.com/search?q=%23locust-prerequisites)
    - [Writing the Locustfile](https://www.google.com/search?q=%23writing-the-locustfile)
    - [Running a Locust Test](https://www.google.com/search?q=%23running-a-locust-test)
- [API Testing with Streamlit](https://www.google.com/search?q=%23api-testing-with-streamlit)
    - [Streamlit Prerequisites](https://www.google.com/search?q=%23streamlit-prerequisites)
    - [Running the Streamlit Application](https://www.google.com/search?q=%23running-the-streamlit-application)
- [Troubleshooting](https://www.google.com/search?q=%23troubleshooting)

-----

## Project Overview

The `trust-registry-api` is a Spring Boot application that provides a RESTful interface for a Hyperledger Fabric chaincode. It uses the Hyperledger Fabric Gateway Java SDK to connect to the network, submit transactions (e.g., creating records), and evaluate queries (e.g., reading records).

-----

## Prerequisites

Ensure you have the following installed on your system:

* **Java Development Kit (JDK) 17 or higher:**
    * Verify with: `java -version`
* **Apache Maven 3.6.3 or higher:**
    * Verify with: `mvn -v`
* **Docker Desktop:** Required for running the Hyperledger Fabric network.
    * Verify with: `docker --version`
* **Hyperledger Fabric Samples (v2.x or v2.5.x recommended):** This project assumes you have cloned and configured `fabric-samples`, specifically the `test-network`.
    * Download from: [Hyperledger Fabric Samples GitHub](https://github.com/hyperledger/fabric-samples)
* **Your deployed Trust Registry Chaincode (Go language):** This README assumes you have the `smartcontract.go` (or similar Go chaincode) ready to be deployed or already deployed on your Fabric network.
* **Postman (or similar API client):** Highly recommended for testing the REST API endpoints.

-----

## Setup and Running Instructions

Follow these steps to bring up the Hyperledger Fabric network, deploy the chaincode, and run the Spring Boot API.

### 1\. Set Up Hyperledger Fabric Network

Navigate to your `fabric-samples/test-network` directory in your terminal and execute the following commands to bring up the network, create a channel, and set up the Certificate Authorities (CAs).

```bash
cd /path/to/fabric-samples/test-network # Adjust this path to your actual setup
./network.sh down # Ensure a clean start
./network.sh up createChannel -c trust-registry-channel -ca
```

This command will:

* Bring down any existing Fabric containers.
* Bring up a new Fabric network (including peers, orderers, and CAs).
* Create a channel named `trust-registry-channel`.

### 2\. Deploy the Trust Registry Chaincode

After the network is up, you need to deploy your Trust Registry chaincode to the `trust-registry-channel`. This example assumes your Go chaincode source is located relative to your `test-network` directory.

```bash
# Example: Assuming your chaincode is in a 'chaincode-go' directory peer to trust-registry-java-rest-api
# And trust-registry-java-rest-api is peer to fabric-samples
./network.sh deployCC -ccn trustregistry -ccp ../trust-registry-java-rest-api/chaincode-go -ccl go -c trust-registry-channel
```

* **Important:** Adjust the `-ccp` (chaincode path) argument to the actual location of your Go chaincode source code.
* `-ccn trustregistry`: Specifies the chaincode name as `trustregistry`, which must match the `fabric.network.chaincode-name` in your Spring Boot `application.properties`.
* `-ccl go`: Indicates the chaincode language is Go.
* `-c trust-registry-channel`: Specifies the channel to deploy the chaincode on.

### 3\. Configure the Spring Boot Application

Before running the Spring Boot application, ensure its configuration aligns with your Fabric network setup.

**a. `application.properties` Configuration:**
Open `src/main/resources/application.properties` and verify the following properties. **Pay close attention to `fabric.network.crypto-base-path` as it's system-specific.**

```properties
# src/main/resources/application.properties
fabric.network.channel-name=trust-registry-channel
fabric.network.chaincode-name=trustregistry
fabric.network.msp-id=Org1MSP

# IMPORTANT: Adjust this path to your actual fabric-samples/test-network/organizations directory
fabric.network.crypto-base-path=/Users/kumhosur/Documents/hyperledger-fabric-practise/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com

fabric.network.peer-endpoint=localhost:7051
fabric.network.override-authority=peer0.org1.example.com
```

**b. Java Model Classes (`@JsonProperty` Annotations):**
Your Go chaincode uses `snake_case` for JSON fields (e.g., `created_at`), while Java `camelCase` by default. To ensure proper serialization and deserialization, you **must** add `@JsonProperty` annotations from `com.fasterxml.jackson.annotation.JsonProperty` to your Java model classes (`GovernanceRecord.java` and `TrustRecord.java`).

**`src/main/java/com/example/trustregistry/model/GovernanceRecord.java`:**

```java
package com.example.trustregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("deleted_at")
    private String deletedAt;
    private String identifier;
    private String name;
    private String status;
}
```

**`src/main/java/com/example/trustregistry/model/TrustRecord.java`:**

```java
package com.example.trustregistry.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustRecord {
    private String id;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("deleted_at")
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
```

### 4\. Build and Run the Spring Boot Application

Navigate to the root directory of your Spring Boot project (`trust-registry-api`) in your terminal and run the following Maven commands:

```bash
mvn clean install # Cleans, compiles, tests, and packages the application
mvn spring-boot:run # Runs the Spring Boot application
```

The application will start on `http://localhost:8080`. You should see a log message similar to "Fabric Gateway Client initialized successfully." indicating a successful connection to the Fabric network.

-----

## API Usage (with Postman)

Once the Spring Boot application is running, you can interact with it using Postman or any other API client.

### Importing Postman Collection

For convenience, you can import the following JSON directly into Postman. This collection contains pre-configured requests for all available endpoints, including pre-request scripts to generate unique IDs and timestamps.

1.  Open Postman.
2.  Click on "Import" (usually in the top left corner).
3.  Select "Raw text" and paste the JSON below.
4.  Click "Continue" and then "Import".

<!-- end list -->

```json
{
    "info": {
       "_postman_id": "YOUR_POSTMAN_COLLECTION_ID_HERE",
       "name": "Trust Registry API",
       "schema": "[https://schema.getpostman.com/json/collection/v2.1.0/collection.json](https://schema.getpostman.com/json/collection/v2.1.0/collection.json)",
       "_collection_link": "[https://www.postman.com/collections/YOUR_COLLECTION_ID](https://www.postman.com/collections/YOUR_COLLECTION_ID)"
    },
    "item": [
       {
          "name": "Initialize Ledger (Required First)",
          "request": {
             "method": "POST",
             "header": [],
             "url": {
                "raw": "http://localhost:8080/api/trustregistry/initledger",
                "protocol": "http",
                "host": [
                   "localhost"
                ],
                "port": "8080",
                "path": [
                   "api",
                   "trustregistry",
                   "initledger"
                ]
             }
          },
          "response": []
       },
       {
          "name": "Governance Records",
          "item": [
             {
                "name": "Create Governance Record",
                "event": [
                   {
                      "listen": "prerequest",
                      "script": {
                         "exec": [
                            "// Generate a UUID",
                            "function generateUUID() {",
                            "    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {",
                            "        var r = Math.random() * 16 | 0,",
                            "            v = c == 'x' ? r : (r & 0x3 | 0x8);",
                            "        return v.toString(16);",
                            "    });",
                            "}",
                            "",
                            "// Generate current ISO 8601 timestamp (UTC)",
                            "function generateISOtimestamp() {",
                            "    return new Date().toISOString();",
                            "}",
                            "",
                            "// Set the generated UUIDs as collection/environment variables",
                            "pm.collectionVariables.set(\"governanceId\", generateUUID());",
                            "pm.collectionVariables.set(\"trustId\", generateUUID());",
                            "",
                            "// Set the generated timestamps as collection/environment variables",
                            "pm.collectionVariables.set(\"currentTimestamp\", generateISOtimestamp());",
                            "",
                            "// Log for debugging",
                            "console.log(\"Generated Governance ID: \" + pm.collectionVariables.get(\"governanceId\"));",
                            "console.log(\"Generated Trust ID: \" + pm.collectionVariables.get(\"trustId\"));",
                            "console.log(\"Generated Timestamp: \" + pm.collectionVariables.get(\"currentTimestamp\"));"
                         ],
                         "type": "text/javascript"
                      }
                   }
                ],
                "request": {
                   "method": "POST",
                   "header": [
                      {
                         "key": "Content-Type",
                         "value": "application/json"
                      }
                   ],
                   "body": {
                      "mode": "raw",
                      "raw": "{\n    \"id\": \"{{governanceId}}\",\n    \"created_at\": \"{{currentTimestamp}}\",\n    \"updated_at\": \"{{currentTimestamp}}\",\n    \"deleted_at\": null,\n    \"identifier\": \"did:web:governance-framework-id-{{$randomInt}}\",\n    \"name\": \"MyGeneratedGovernanceFramework\",\n    \"status\": \"Active\"\n}"
                   },
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/governance",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "governance"
                      ]
                   }
                },
                "response": []
             },
             {
                "name": "Read Governance Record by Identifier",
                "request": {
                   "method": "GET",
                   "header": [],
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/governance/did:web:governance-framework-id-123",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "governance",
                         "did:web:governance-framework-id-123"
                      ]
                   }
                },
                "response": []
             },
             {
                "name": "Get All Governance Records",
                "request": {
                   "method": "GET",
                   "header": [],
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/governance",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "governance"
                      ]
                   }
                },
                "response": []
             }
          ]
       },
       {
          "name": "Trust Records",
          "item": [
             {
                "name": "Create Trust Record",
                "event": [
                   {
                      "listen": "prerequest",
                      "script": {
                         "exec": [
                            "// Generate a UUID",
                            "function generateUUID() {",
                            "    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {",
                            "        var r = Math.random() * 16 | 0,",
                            "            v = c == 'x' ? r : (r & 0x3 | 0x8);",
                            "        return v.toString(16);",
                            "    });",
                            "}",
                            "",
                            "// Generate current ISO 8601 timestamp (UTC)",
                            "function generateISOtimestamp() {",
                            "    return new Date().toISOString();",
                            "}",
                            "",
                            "// Set the generated UUIDs as collection/environment variables",
                            "pm.collectionVariables.set(\"governanceId\", generateUUID());",
                            "pm.collectionVariables.set(\"trustId\", generateUUID());",
                            "",
                            "// Set the generated timestamps as collection/environment variables",
                            "pm.collectionVariables.set(\"currentTimestamp\", generateISOtimestamp());",
                            "",
                            "// Log for debugging",
                            "console.log(\"Generated Governance ID: \" + pm.collectionVariables.get(\"governanceId\"));",
                            "console.log(\"Generated Trust ID: \" + pm.collectionVariables.get(\"trustId\"));",
                            "console.log(\"Generated Timestamp: \" + pm.collectionVariables.get(\"currentTimestamp\"));"
                         ],
                         "type": "text/javascript"
                      }
                   }
                ],
                "request": {
                   "method": "POST",
                   "header": [
                      {
                         "key": "Content-Type",
                         "value": "application/json"
                      }
                   ],
                   "body": {
                      "mode": "raw",
                      "raw": "{\n    \"id\": \"{{trustId}}\",\n    \"created_at\": \"{{currentTimestamp}}\",\n    \"updated_at\": \"{{currentTimestamp}}\",\n    \"deleted_at\": null,\n    \"identifier\": \"did:web:governance-framework-id-123\",\n    \"entity_type\": \"LegalPerson\",\n    \"credential_type\": \"VerifiableCredential\",\n    \"governance_framework_uri\": \"[https://example.com/framework/v1](https://example.com/framework/v1)\",\n    \"did_document\": \"{ \\\"id\\\": \\\"did:example:123\\\" }\",\n    \"valid_from_dt\": \"2025-01-01T00:00:00Z\",\n    \"valid_until_dt\": \"2026-01-01T00:00:00Z\",\n    \"status\": \"Active\",\n    \"status_detail\": \"Verified\"\n}"
                   },
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/trust",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "trust"
                      ]
                   }
                },
                "response": []
             },
             {
                "name": "Read Trust Record by ID",
                "request": {
                   "method": "GET",
                   "header": [],
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/trust/trust1",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "trust",
                         "trust1"
                      ]
                   }
                },
                "response": []
             },
             {
                "name": "Get All Trust Records",
                "request": {
                   "method": "GET",
                   "header": [],
                   "url": {
                      "raw": "http://localhost:8080/api/trustregistry/trust",
                      "protocol": "http",
                      "host": [
                         "localhost"
                      ],
                      "port": "8080",
                      "path": [
                         "api",
                         "trustregistry",
                         "trust"
                      ]
                   }
                },
                "response": []
             }
          ]
       }
    ]
}
```

### Using Pre-request Scripts for IDs and Timestamps

The Postman collection includes "Pre-request Scripts" for "Create Governance Record" and "Create Trust Record" requests. These scripts automatically generate a UUID for the `id` field and the current UTC timestamp (ISO 8601 format) for `created_at` and `updated_at` before the request is sent.

Variables set by the script (and used in the JSON request body):

* `{{governanceId}}`: A unique ID for governance records.
* `{{trustId}}`: A unique ID for trust records.
* `{{currentTimestamp}}`: The current timestamp in ISO 8601 format (e.g., `2025-07-29T11:05:00.000Z`).

This ensures uniqueness and proper timestamp formats without manual entry.

### Endpoints Overview

All API endpoints are prefixed with `/api/trustregistry`.

* **`POST /api/trustregistry/initledger`**: Initializes the ledger by calling the `InitLedger` chaincode function. **Run this first\!**
* **`POST /api/trustregistry/governance`**: Creates a new `GovernanceRecord`.
* **`GET /api/trustregistry/governance/{identifier}`**: Reads a `GovernanceRecord` by its unique identifier.
* **`GET /api/trustregistry/governance`**: Retrieves all `GovernanceRecord`s.
* **`POST /api/trustregistry/trust`**: Creates a new `TrustRecord`.
* **`GET /api/trustregistry/trust/{id}`**: Reads a `TrustRecord` by its unique ID.
* **`GET /api/trustregistry/trust`**: Retrieves all `TrustRecord`s.

Refer to the Postman collection for detailed request bodies and example responses.

-----

## Performance Testing with Locust

Locust is an open-source load testing tool. It allows you to define user behavior in Python and swarm your API with a massive number of concurrent users.

### Locust Prerequisites

1.  **Install Python 3.7+**.

2.  **Install Locust** using pip.

    ```bash
    pip install locust
    ```

### Writing the Locustfile

Create a file named `locustfile.py` in the root of your project. This file defines the API endpoints to test and the behavior of your simulated users.

```python
from locust import HttpUser, task, between
import json
import uuid
import random

class TrustRegistryUser(HttpUser):
    wait_time = between(1, 5)  # Wait between 1 and 5 seconds between tasks

    @task(1)
    def create_governance_record(self):
        """Creates a new governance record."""
        # Use a random integer to create a unique identifier
        unique_id = str(random.randint(10000, 99999))
        payload = {
            "id": str(uuid.uuid4()),
            "created_at": "2025-07-29T11:05:00Z",
            "updated_at": "2025-07-29T11:05:00Z",
            "deleted_at": None,
            "identifier": f"did:web:locust-test-org-{unique_id}",
            "name": f"Locust Test Org {unique_id}",
            "status": "Active"
        }
        self.client.post("/api/trustregistry/governance", json=payload)

    @task(2)
    def get_all_governance_records(self):
        """Retrieves all governance records."""
        self.client.get("/api/trustregistry/governance")

    @task(3)
    def create_trust_record(self):
        """Creates a new trust record."""
        # For this example, we assume a pre-existing governance record identifier
        # In a real test, you would dynamically retrieve or pre-load these
        governance_identifier = "did:web:locust-test-org-12345"
        payload = {
            "id": str(uuid.uuid4()),
            "created_at": "2025-07-29T11:05:00Z",
            "updated_at": "2025-07-29T11:05:00Z",
            "deleted_at": None,
            "identifier": governance_identifier,
            "credential_type": "VerifiableCredential",
            "status": "Active"
        }
        self.client.post("/api/trustregistry/trust", json=payload)

    @task(2)
    def get_all_trust_records(self):
        """Retrieves all trust records."""
        self.client.get("/api/trustregistry/trust")
```

### Running a Locust Test

1.  Start your Spring Boot application.

2.  Open a new terminal, navigate to the directory containing `locustfile.py`, and run the following command:

    ```bash
    locust -f locust_loadtest.py --host=http://localhost:8080
    ```

3.  Open your browser and go to `http://localhost:8089` to access the Locust web UI.

4.  Configure your test parameters (e.g., number of users, spawn rate) and click "Start swarming" to begin the performance test.

-----

## API Testing with Streamlit

Streamlit is an open-source app framework for creating beautiful, custom web apps for machine learning and data science. Here, it provides a simple UI to manually test your API endpoints.

### Streamlit Prerequisites

1.  **Install Python 3.7+**.

2.  **Install Streamlit** and the `requests` library.

    ```bash
    pip install streamlit requests
    ```

### Running the Streamlit Application

1.  **Create a Python script** named `streamlit_api_testing.py` in your project root. Paste the following code into it. This script includes a UI for each API endpoint.

    ```python
    import streamlit as st
    import requests
    import json
    import uuid
    from datetime import datetime, timezone

    # Your API base URL
    API_BASE_URL = "http://localhost:8080/api/trustregistry"

    st.title("Trust Registry API Tester 🚀")

    def create_governance_record():
        st.header("Create Governance Record")
        st.caption("This will create a new governance record.")

        with st.form("create_governance_form"):
            identifier = st.text_input("Identifier", "did:web:new-flight-operator.io:1")
            name = st.text_input("Name", "New Flight Operator")
            status = st.selectbox("Status", ["Active", "Inactive"])

            submitted = st.form_submit_button("Create Record")
            if submitted:
                now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

                record_data = {
                    "id": str(uuid.uuid4()),
                    "created_at": now,
                    "updated_at": now,
                    "deleted_at": None,
                    "identifier": identifier,
                    "name": name,
                    "status": status,
                }

                st.write("### Request Payload being sent:")
                st.json(record_data)

                try:
                    response = requests.post(f"{API_BASE_URL}/governance", json=record_data)
                    st.write("---")
                    if response.status_code == 200:
                        st.success("Governance Record created successfully!")
                        st.json(response.json())
                    else:
                        st.error(f"Error: {response.status_code}")
                        st.json(response.json())
                except requests.exceptions.ConnectionError:
                    st.error("Connection Error: Is your backend server running?")

    def get_governance_records():
        st.header("Get All Governance Records")
        st.caption("This will fetch all governance records from the ledger.")
        if st.button("Get All Records"):
            try:
                response = requests.get(f"{API_BASE_URL}/governance")
                st.write("---")
                if response.status_code == 200:
                    st.success("Successfully retrieved all governance records!")
                    st.json(response.json())
                else:
                    st.error(f"Error: {response.status_code}")
                    st.json(response.json())
            except requests.exceptions.ConnectionError:
                st.error("Connection Error: Is your backend server running?")

    def read_governance_record():
        st.header("Read a Specific Governance Record")
        st.caption("Retrieve a governance record by its identifier.")

        with st.form("read_governance_form"):
            identifier = st.text_input("Identifier", "did:web:initial-did.io:kumaraswami.hosuru:1")
            submitted = st.form_submit_button("Read Record")
            if submitted:
                try:
                    response = requests.get(f"{API_BASE_URL}/governance/{identifier}")
                    st.write("---")
                    if response.status_code == 200:
                        st.success("Governance Record found!")
                        st.json(response.json())
                    else:
                        st.error(f"Error: {response.status_code}")
                        st.json(response.json())
                except requests.exceptions.ConnectionError:
                    st.error("Connection Error: Is your backend server running?")

    def create_trust_record():
        st.header("Create Trust Record")
        st.caption("This will create a new trust record.")

        with st.form("create_trust_form"):
            identifier = st.text_input("Identifier (from Governance Record)", "did:web:flight-service-operator:kumaraswami.hosuru:1")
            credential_type = st.text_input("Credential Type", "VerifiableCredential")
            status = st.selectbox("Status", ["Active", "Revoked"])

            submitted = st.form_submit_button("Create Record")
            if submitted:
                now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
                record_data = {
                    "id": str(uuid.uuid4()),
                    "created_at": now,
                    "updated_at": now,
                    "deleted_at": None,
                    "credential_type": credential_type,
                    "identifier": identifier,
                    "status": status,
                }

                st.write("### Request Payload being sent:")
                st.json(record_data)

                try:
                    response = requests.post(f"{API_BASE_URL}/trust", json=record_data)
                    st.write("---")
                    if response.status_code == 200:
                        st.success("Trust Record created successfully!")
                        st.json(response.json())
                    else:
                        st.error(f"Error: {response.status_code}")
                        st.json(response.json())
                except requests.exceptions.ConnectionError:
                    st.error("Connection Error: Is your backend server running?")

    def get_trust_records():
        st.header("Get All Trust Records")
        st.caption("This will fetch all trust records from the ledger.")
        if st.button("Get All Trust Records"):
            try:
                response = requests.get(f"{API_BASE_URL}/trust")
                st.write("---")
                if response.status_code == 200:
                    st.success("Successfully retrieved all trust records!")
                    st.json(response.json())
                else:
                    st.error(f"Error: {response.status_code}")
                    st.json(response.json())
            except requests.exceptions.ConnectionError:
                st.error("Connection Error: Is your backend server running?")

    def read_trust_record():
        st.header("Read a Specific Trust Record")
        st.caption("Retrieve a trust record by its ID.")
        with st.form("read_trust_form"):
            record_id = st.text_input("Trust Record ID", "")
            submitted = st.form_submit_button("Read Record")
            if submitted:
                try:
                    response = requests.get(f"{API_BASE_URL}/trust/{record_id}")
                    st.write("---")
                    if response.status_code == 200:
                        st.success(f"Trust record found for ID: {record_id}")
                        st.json(response.json())
                    else:
                        st.error(f"Error: {response.status_code}")
                        st.json(response.json())
                except requests.exceptions.ConnectionError:
                    st.error("Connection Error: Is your backend server running?")

    def get_trust_records_by_type():
        st.header("Get Trust Records by Credential Type")
        st.caption("Retrieve trust records based on a specific credential type.")
        with st.form("get_by_type_form"):
            credential_type = st.text_input("Credential Type", "VerifiableCredential")
            submitted = st.form_submit_button("Get Records by Type")
            if submitted:
                try:
                    response = requests.get(f"{API_BASE_URL}/trust/credential_type/{credential_type}")
                    st.write("---")
                    if response.status_code == 200:
                        st.success(f"Records with type '{credential_type}' found!")
                        st.json(response.json())
                    else:
                        st.error(f"Error: {response.status_code}")
                        st.json(response.json())
                except requests.exceptions.ConnectionError:
                    st.error("Connection Error: Is your backend server running?")

    # Sidebar navigation
    st.sidebar.title("API Endpoints")
    page = st.sidebar.radio("Select an API", [
        "Create Governance Record",
        "Read Governance Record",
        "Get All Governance Records",
        "Create Trust Record",
        "Read Trust Record",
        "Get All Trust Records",
        "Get Trust Records by Credential Type"
    ])

    if page == "Create Governance Record":
        create_governance_record()
    elif page == "Read Governance Record":
        read_governance_record()
    elif page == "Get All Governance Records":
        get_governance_records()
    elif page == "Create Trust Record":
        create_trust_record()
    elif page == "Read Trust Record":
        read_trust_record()
    elif page == "Get All Trust Records":
        get_trust_records()
    elif page == "Get Trust Records by Credential Type":
        get_trust_records_by_type()
    ```

2.  **Run the Streamlit app** from your terminal in the same directory:

    ```bash
    streamlit run streamlit_api_testing.py
    ```

3.  The application will open in your web browser, providing a user-friendly interface to test your API.

-----

## Troubleshooting

* **`com.google.protobuf.RuntimeVersion$ProtobufRuntimeVersionException: Detected incompatible Protobuf Gencode/Runtime versions`**:

    * **Cause:** Mismatch between the Protobuf version used to compile Fabric protos and the runtime version in your Spring Boot application.
    * **Solution:** Ensure `protobuf-java.version` in your `pom.xml` (currently `4.28.2`) matches or is compatible with the Fabric SDK's requirements. Running `mvn clean install` and clearing local Maven caches (`rm -rf ~/.m2/repository/com/google/protobuf`) can sometimes help.

* **`404 Not Found` for endpoints (e.g., `/api/initledger`)**:

    * **Cause:** The base mapping for your `TrustRegistryController` is `/api/trustregistry`. Endpoints like `initledger` should be accessed under this path.
    * **Solution:** Use the correct full URL, e.g., `http://localhost:8080/api/trustregistry/initledger`.

* **`ABORTED: failed to endorse transaction, chaincode response 500, governance record CreatedAt cannot be empty` (or similar for other fields)**:

    * **Cause:** This critical error indicates a data validation failure within your Go chaincode. It typically happens when the JSON sent from your Spring Boot app does not match the chaincode's expected field names or data requirements.
    * **Solution:**
        1.  **Verify `@JsonProperty` annotations:** Ensure all fields in your Java model classes (`GovernanceRecord.java`, `TrustRecord.java`) that correspond to snake\_case JSON fields in your Go chaincode have the `@JsonProperty("snake_case_name")` annotation.
        2.  **Check JSON payload:** Confirm that the JSON request body you are sending (e.g., from Postman) uses `snake_case` for fields like `created_at`, `updated_at`, `entity_type`, etc., as defined in your `smartcontract.go`. The provided Postman collection does this.
        3.  **Ensure required fields are not empty/null:** For fields like `created_at` that your chaincode expects to be non-empty, ensure they are populated with valid data. The Postman pre-request script helps with this for IDs and timestamps.

* **Error: `context finished before block retrieved: context canceled` in peer logs**:

    * **Cause:** This typically signifies a transient network timeout or communication hiccup between the Fabric Gateway SDK and the peer/orderer.
    * **Solution:** Retrying the transaction often resolves this. If it persists, inspect your Docker network setup for any connectivity issues or consider increasing the Gateway client's default timeouts in `FabricGatewayClient.java` if transactions are very slow.
