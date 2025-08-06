//package com.example.trustregistry.controller;
//
//import com.example.trustregistry.FabricGatewayClient;
//import com.example.trustregistry.model.GovernanceRecord;
//import com.example.trustregistry.model.TrustRecord;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.concurrent.TimeoutException;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class TrustRegistryControllerTest {
//
//    @Mock
//    private FabricGatewayClient fabricGatewayClient;
//
//    @Mock
//    private ObjectMapper objectMapper;
//
//    @InjectMocks
//    private TrustRegistryController trustRegistryController;
//
//    @BeforeEach
//    void setUp() {
//        // Setup code if needed
//    }
//
//    @Test
//    void testInitLedgerSuccess() throws Exception {
//        when(fabricGatewayClient.submitTransaction("InitLedger")).thenReturn("Success");
//        ResponseEntity<Object> response = trustRegistryController.initLedger();
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Ledger initialized: Success", response.getBody());
//    }
//
//    @Test
//    void testInitLedgerTimeoutException() throws Exception {
//        when(fabricGatewayClient.submitTransaction("InitLedger")).thenThrow(new TimeoutException("Timeout"));
//        ResponseEntity<Object> response = trustRegistryController.initLedger();
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    @Test
//    void testCreateGovernanceRecordSuccess() throws Exception {
//        GovernanceRecord record = new GovernanceRecord();
//        String recordJson = "{}";
//        when(objectMapper.writeValueAsString(record)).thenReturn(recordJson);
//        when(fabricGatewayClient.submitTransaction("CreateGovernanceRecord", recordJson)).thenReturn("Success");
//        ResponseEntity<Object> response = trustRegistryController.createGovernanceRecord(record);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Governance Record created successfully: Success", response.getBody());
//    }
//
//    @Test
//    void testCreateGovernanceRecordAlreadyExists() throws Exception {
//        GovernanceRecord record = new GovernanceRecord();
//        String recordJson = "{}";
//        when(objectMapper.writeValueAsString(record)).thenReturn(recordJson);
//        when(fabricGatewayClient.submitTransaction("CreateGovernanceRecord", recordJson))
//                .thenThrow(new Exception("already exists"));
//        ResponseEntity<Object> response = trustRegistryController.createGovernanceRecord(record);
//        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
//    }
//
//    @Test
//    void testReadGovernanceRecordSuccess() throws Exception {
//        String identifier = "gov123";
//        when(fabricGatewayClient.evaluateTransaction("ReadGovernanceRecord", identifier)).thenReturn("Record Data");
//        ResponseEntity<Object> response = trustRegistryController.readGovernanceRecord(identifier);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Record Data", response.getBody());
//    }
//
//    @Test
//    void testReadGovernanceRecordNotFound() throws Exception {
//        String identifier = "gov123";
//        when(fabricGatewayClient.evaluateTransaction("ReadGovernanceRecord", identifier))
//                .thenThrow(new Exception("does not exist"));
//        ResponseEntity<Object> response = trustRegistryController.readGovernanceRecord(identifier);
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetAllGovernanceRecordsSuccess() throws Exception {
//        when(fabricGatewayClient.evaluateTransaction("GetAllGovernanceRecords")).thenReturn("All Records");
//        ResponseEntity<Object> response = trustRegistryController.getAllGovernanceRecords();
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("All Records", response.getBody());
//    }
//
//    @Test
//    void testCreateTrustRecordSuccess() throws Exception {
//        TrustRecord record = new TrustRecord();
//        String recordJson = "{}";
//        when(objectMapper.writeValueAsString(record)).thenReturn(recordJson);
//        when(fabricGatewayClient.submitTransaction("CreateTrustRecord", recordJson)).thenReturn("Success");
//        ResponseEntity<Object> response = trustRegistryController.createTrustRecord(record);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Trust Record created successfully: Success", response.getBody());
//    }
//
//    @Test
//    void testReadTrustRecordSuccess() throws Exception {
//        String id = "trust123";
//        when(fabricGatewayClient.evaluateTransaction("ReadTrustRecord", id)).thenReturn("Trust Record Data");
//        ResponseEntity<Object> response = trustRegistryController.readTrustRecord(id);
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Trust Record Data", response.getBody());
//    }
//
//    @Test
//    void testReadTrustRecordNotFound() throws Exception {
//        String id = "trust123";
//        when(fabricGatewayClient.evaluateTransaction("ReadTrustRecord", id))
//                .thenThrow(new Exception("does not exist"));
//        ResponseEntity<Object> response = trustRegistryController.readTrustRecord(id);
//        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
//    }
//
//    @Test
//    void testGetAllTrustRecordsSuccess() throws Exception {
//        when(fabricGatewayClient.evaluateTransaction("GetAllTrustRecords")).thenReturn("All Trust Records");
//        ResponseEntity<Object> response = trustRegistryController.getAllTrustRecords();
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("All Trust Records", response.getBody());
//    }
//}
