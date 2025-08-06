package com.example.trustregistry.controller;

import com.example.trustregistry.FabricGatewayClient;
import com.example.trustregistry.model.ErrorResponse;
import com.example.trustregistry.model.GovernanceRecord;
import com.example.trustregistry.model.TrustRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/trustregistry")
public class TrustRegistryController {

    private static final Logger logger = LoggerFactory.getLogger(TrustRegistryController.class);
    private final FabricGatewayClient fabricGatewayClient;
    private final ObjectMapper objectMapper;

    private static final Pattern CHAINCODE_ERROR_PATTERN = Pattern.compile("chaincode response \\d{3}, (.*)");

    @Autowired
    public TrustRegistryController(FabricGatewayClient fabricGatewayClient, ObjectMapper objectMapper) {
        this.fabricGatewayClient = fabricGatewayClient;
        this.objectMapper = objectMapper;
    }

    private String getSystemIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Unable to get system IP address", e);
            return "unknown";
        }
    }

    private ResponseEntity<Object> createErrorResponse(HttpStatus status, String message, String details, String correlationId) {
        try {
            ErrorResponse errorResponse = new ErrorResponse(message, details);
            logger.error("ErrorResponse created with correlationId: {}, message: {}, details: {}", correlationId, message, details);
            return ResponseEntity.status(status)
                    .body(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            logger.error("Error serializing ErrorResponse with correlationId: {}", correlationId, e);
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("message", "An unexpected error occurred during error serialization");
            errorMap.put("details", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMap);
        }
    }

    private String extractChaincodeErrorMessage(String chaincodeErrorMessage) {
        Matcher matcher = CHAINCODE_ERROR_PATTERN.matcher(chaincodeErrorMessage);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return chaincodeErrorMessage;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private void logRequest(String action, String systemIp, String correlationId) {
        logger.info("{} request received from IP: {}, correlationId: {}", action, systemIp, correlationId);
    }

    // New helper method to handle submitting a transaction
    private ResponseEntity<Object> submitTransaction(String transactionName, String payload, String correlationId) {
        String systemIp = getSystemIpAddress();
        logRequest(transactionName, systemIp, correlationId);
        try {
            String result = fabricGatewayClient.submitTransaction(transactionName, payload);

            logger.info("{} completed successfully, correlationId: {}", transactionName, correlationId);

            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("result", "Success");

            if ("CreateGovernanceRecord".equals(transactionName) || "CreateTrustRecord".equals(transactionName)) {
                successResponse.put("message", transactionName + " created successfully for ID: " + result);
            } else {
                successResponse.put("message", transactionName + " completed successfully with ID: " + result);
            }

            return ResponseEntity.ok(successResponse);
        } catch (TimeoutException | InterruptedException e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Error processing transaction '%s' due to timeout or interruption", transactionName),
                    e.getMessage(), correlationId);
        } catch (Exception e) {
            String detailedError = extractChaincodeErrorMessage(e.getMessage());

            if ("CreateGovernanceRecord".equals(transactionName) && detailedError.contains("already exists")) {
                return createErrorResponse(HttpStatus.CONFLICT, "Governance record already exists", detailedError, correlationId);
            } else if ("CreateTrustRecord".equals(transactionName) && detailedError.contains("governance record not found")) {
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Trust record validation failed: Associated governance record not found", detailedError, correlationId);
            }
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    String.format("Invalid data or chaincode error for transaction '%s'", transactionName),
                    detailedError, correlationId);
        }
    }

    // New helper method to handle evaluating a transaction (queries)
    private ResponseEntity<Object> evaluateTransaction(String transactionName, String correlationId, String... args) {
        String systemIp = getSystemIpAddress();
        logRequest(transactionName, systemIp, correlationId);
        try {
            String result;
            if (args.length > 0) {
                result = fabricGatewayClient.evaluateTransaction(transactionName, args[0]);
            } else {
                result = fabricGatewayClient.evaluateTransaction(transactionName);
            }

            logger.info("{} completed successfully, correlationId: {}", transactionName, correlationId);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            String detailedError = extractChaincodeErrorMessage(e.getMessage());

            if (("ReadGovernanceRecord".equals(transactionName) || "ReadTrustRecord".equals(transactionName)) && detailedError.contains("does not exist")) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Record not found", detailedError, correlationId);
            }

            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    String.format("Error processing transaction '%s'", transactionName),
                    detailedError, correlationId);
        }
    }

    /**
     * Helper method for evaluating a transaction with a single string argument.
     *
     * @param transactionName The name of the chaincode transaction to evaluate.
     * @param argument The single string argument for the transaction.
     * @return A ResponseEntity with the transaction result or an error.
     */
    private ResponseEntity<Object> evaluateSingleArgTransaction(String transactionName, String argument) {
        String correlationId = generateCorrelationId();
        return evaluateTransaction(transactionName, correlationId, argument);
    }

    @PostMapping("/governance")
    public ResponseEntity<Object> createGovernanceRecord(@RequestBody GovernanceRecord record) throws JsonProcessingException {
        String correlationId = generateCorrelationId();
        String recordJson = objectMapper.writeValueAsString(record);
        return submitTransaction("CreateGovernanceRecord", recordJson, correlationId);
    }

    @PostMapping("/initledger")
    public ResponseEntity<Object> initLedger(@RequestBody GovernanceRecord initialRecord) throws JsonProcessingException {
        String correlationId = generateCorrelationId();
        String initialRecordJson = objectMapper.writeValueAsString(initialRecord);
        return submitTransaction("InitLedger", initialRecordJson, correlationId);
    }

    @GetMapping("/governance/{identifier}")
    public ResponseEntity<Object> readGovernanceRecord(@PathVariable String identifier) {
        String correlationId = generateCorrelationId();
        return evaluateTransaction("ReadGovernanceRecord", correlationId, identifier);
    }

    @GetMapping("/governance")
    public ResponseEntity<Object> getAllGovernanceRecords() {
        String correlationId = generateCorrelationId();
        return evaluateTransaction("GetAllGovernanceRecords", correlationId);
    }

    @PostMapping("/trust")
    public ResponseEntity<Object> createTrustRecord(@RequestBody TrustRecord record) throws JsonProcessingException {
        String correlationId = generateCorrelationId();
        String recordJson = objectMapper.writeValueAsString(record);
        return submitTransaction("CreateTrustRecord", recordJson, correlationId);
    }

    @GetMapping("/trust/{id}")
    public ResponseEntity<Object> readTrustRecord(@PathVariable String id) {
        return evaluateSingleArgTransaction("ReadTrustRecord", id);
    }


    @GetMapping("/trust")
    public ResponseEntity<Object> getAllTrustRecords() {
        String correlationId = generateCorrelationId();
        return evaluateTransaction("GetAllTrustRecords", correlationId);
    }

    @GetMapping("/trust/credential_type/{credentialType}")
    public ResponseEntity<Object> getTrustRecordsByCredentialType(@PathVariable String credentialType) {
        return evaluateSingleArgTransaction("GetTrustRecordsByCredentialType", credentialType);
    }
}
