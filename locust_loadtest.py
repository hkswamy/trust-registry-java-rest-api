import uuid
import json
from datetime import datetime
from locust import HttpUser, task, between

class TrustRegistryUser(HttpUser):
    # Simulate a user waiting between 1 and 5 seconds after each task
    wait_time = between(1, 5)

    # Store data created by a user to be used in other tasks
    governance_id = None
    trust_id = None

    def on_start(self):
        """
        Called when a new user is spawned. This user will create a new
        governance record that it can then read in subsequent tasks.
        """
        print("Spawning a new user...")
        # Create a unique identifier for this user's session
        self.identifier = f"did:web:psmdp.waltid-mdp.psssiaccelerator.io:test-user:{uuid.uuid4()}"

        # Initialize the governance record for this user
        self.governance_id = str(uuid.uuid4())
        governance_payload = {
            "id": self.governance_id,
            "identifier": self.identifier,
            "name": f"Test Governance Record - {self.governance_id}",
            "status": "Active",
            "created_at": datetime.utcnow().isoformat() + "Z",
            "updated_at": datetime.utcnow().isoformat() + "Z",
            "deleted_at": None
        }

        # Submit the initial governance record.
        # Note: We use a different name to avoid logging it as a regular transaction.
        self.client.post("/api/trustregistry/governance", json=governance_payload, name="0_Init_GovernanceRecord")
        print(f"Created initial governance record with identifier: {self.identifier}")

    # Task 1 (Heavy weight): Create a new TrustRecord.
    # This simulates a high rate of writes to the system.
    @task(3)
    def create_trust_record(self):
        self.trust_id = str(uuid.uuid4())
        trust_payload = {
            "id": self.trust_id,
            "identifier": self.identifier,  # Reuse the pre-created governance identifier
            "schema_uri": f"https://example.com/schema/{uuid.uuid4()}",
            "status": "Active",
            "created_at": datetime.utcnow().isoformat() + "Z",
            "updated_at": datetime.utcnow().isoformat() + "Z",
            "deleted_at": None
        }
        self.client.post("/api/trustregistry/trust", json=trust_payload, name="1_Create_TrustRecord")

    # Task 2 (Medium weight): Get a single GovernanceRecord.
    # This simulates users reading specific records.
    @task(2)
    def get_governance_record(self):
        self.client.get(f"/api/trustregistry/governance/{self.identifier}", name="2_Get_GovernanceRecord")

    # Task 3 (Medium weight): Get a specific TrustRecord.
    # We first need to ensure a trust_id exists.
    @task(2)
    def get_trust_record(self):
        if self.trust_id:
            self.client.get(f"/api/trustregistry/trust/{self.trust_id}", name="3_Get_TrustRecord")
        else:
            # If no trust_id exists yet, skip this task and wait.
            pass

    # Task 4 (Low weight): Get all GovernanceRecords.
    # This simulates an admin-level or dashboard query.
    @task(1)
    def get_all_governance_records(self):
        self.client.get("/api/trustregistry/governance", name="4_Get_All_GovernanceRecords")

    # Task 5 (Low weight): Get all TrustRecords.
    # This also simulates an admin-level query.
    @task(1)
    def get_all_trust_records(self):
        self.client.get("/api/trustregistry/trust", name="5_Get_All_TrustRecords")
