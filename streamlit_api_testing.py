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

            # st.write("### Request Payload being sent:")
            # st.json(record_data)

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

            # st.write("### Request Payload being sent:")
            # st.json(record_data)

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
    "Read Trust Record",  # Added the new option
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
elif page == "Read Trust Record":  # Added the new page logic
    read_trust_record()
elif page == "Get All Trust Records":
    get_trust_records()
elif page == "Get Trust Records by Credential Type":
    get_trust_records_by_type()

