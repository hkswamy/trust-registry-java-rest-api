package com.example.trustregistry;

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.hyperledger.fabric.client.Network;
import org.hyperledger.fabric.client.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Component
public class FabricGatewayClient {

    private static final Logger logger = LoggerFactory.getLogger(FabricGatewayClient.class);

    @Value("${fabric.network.channel-name}")
    private String channelName;

    @Value("${fabric.network.chaincode-name}")
    private String chaincodeName;

    @Value("${fabric.network.msp-id}")
    private String mspId;

    @Value("${fabric.network.crypto-base-path}")
    private String cryptoBasePathString;

    @Value("${fabric.network.peer-endpoint}")
    private String peerEndpoint;

    @Value("${fabric.network.override-authority}")
    private String overrideAuthority;

    private Gateway gateway;
    private Network network;
    private Contract contract;

    private Path cryptoBasePath;
    private Path certDirPath;
    Path keyDirPath;
    private Path tlsCertPath;

    @PostConstruct
    public void init() throws Exception {
        logger.info("Initializing Fabric Gateway Client...");

        // Resolve paths
        cryptoBasePath = Paths.get(cryptoBasePathString);
        certDirPath = cryptoBasePath.resolve(Paths.get("users/User1@org1.example.com/msp/signcerts"));
        keyDirPath = cryptoBasePath.resolve(Paths.get("users/User1@org1.example.com/msp/keystore"));
        tlsCertPath = cryptoBasePath.resolve(Paths.get("peers/peer0.org1.example.com/tls/ca.crt"));

        logger.info("Resolved Crypto Base Path: {}", cryptoBasePath);
        logger.info("Resolved Certificate Directory Path: {}", certDirPath);
        logger.info("Resolved Private Key Directory Path: {}", keyDirPath);
        logger.info("Resolved TLS Certificate Path: {}", tlsCertPath);

        // Check if files exist
        if (!Files.exists(certDirPath) || !Files.isDirectory(certDirPath)) {
            throw new IOException("Certificate directory not found or not a directory: " + certDirPath);
        }
        if (!Files.exists(keyDirPath) || !Files.isDirectory(keyDirPath)) {
            throw new IOException("Private key directory not found or not a directory: " + keyDirPath);
        }
        if (!Files.exists(tlsCertPath) || !Files.isRegularFile(tlsCertPath)) {
            throw new IOException("TLS Certificate file not found or not a file: " + tlsCertPath);
        }

        // The gRPC client connection should be shared by all Gateway connections to this endpoint.
        ManagedChannel channel = newGrpcConnection();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(newIdentity())
                .signer(newSigner())
                .hash(Hash.SHA256)
                .connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        // Create a gateway connection
        gateway = builder.connect();

        // Obtain a smart contract deployed on the network.
        network = gateway.getNetwork(channelName);
        contract = network.getContract(chaincodeName);
        logger.info("Fabric Gateway Client initialized successfully.");
    }

    private ManagedChannel newGrpcConnection() throws IOException {
        var credentials = TlsChannelCredentials.newBuilder()
                .trustManager(tlsCertPath.toFile())
                .build();
        return Grpc.newChannelBuilder(peerEndpoint, credentials)
                .overrideAuthority(overrideAuthority)
                .build();
    }

    private Identity newIdentity() throws IOException, CertificateException {
        try (var certReader = Files.newBufferedReader(getFirstFilePath(certDirPath))) {
            var certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(mspId, certificate);
        }
    }

    private Signer newSigner() throws IOException, InvalidKeyException {
        try (var keyReader = Files.newBufferedReader(getFirstFilePath(keyDirPath))) {
            var privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    private Path getFirstFilePath(Path dirPath) throws IOException {
        try (Stream<Path> keyFiles = Files.list(dirPath)) {
            return keyFiles.findFirst().orElseThrow(() -> new IOException("No files found in directory: " + dirPath));
        }
    }

    public String submitTransaction(String functionName, String... args) throws TimeoutException, InterruptedException, Exception {
        logger.info("Submitting transaction: {}({})", functionName, String.join(", ", args));
        try {
            byte[] result = contract.submitTransaction(functionName, args);
            logger.info("Transaction submitted. Result: {}", new String(result, StandardCharsets.UTF_8));
            return new String(result, StandardCharsets.UTF_8);
        } catch (EndorseException e) {
            String specificError;
            if (e.getDetails() != null && !e.getDetails().isEmpty()) {
                specificError = e.getDetails().get(0).getMessage();
            } else {
                specificError = e.getMessage();
            }
            logger.error("EndorseException during submitTransaction: {}", specificError);
            throw new Exception(specificError, e);
        } catch (GatewayException e) {
            logger.error("Fabric Gateway Exception during submitTransaction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("General Exception during submitTransaction: {}", e.getMessage());
            throw e;
        }
    }

    public String evaluateTransaction(String functionName, String... args) throws Exception {
        logger.info("Evaluating transaction: {}({})", functionName, String.join(", ", args));
        try {
            byte[] result = contract.evaluateTransaction(functionName, args);
            logger.info("Evaluation result: {}", new String(result, StandardCharsets.UTF_8));
            return new String(result, StandardCharsets.UTF_8);
        } catch (EndorseException e) {
            String specificError;
            if (e.getDetails() != null && !e.getDetails().isEmpty()) {
                specificError = e.getDetails().get(0).getMessage();
            } else {
                specificError = e.getMessage();
            }
            logger.error("EndorseException during evaluateTransaction: {}", specificError);
            throw new Exception(specificError, e);
        } catch (GatewayException e) {
            logger.error("Fabric Gateway Exception during evaluateTransaction: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("General Exception during evaluateTransaction: {}", e.getMessage());
            throw e;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (gateway != null) {
            logger.info("Closing Fabric Gateway connection...");
            gateway.close();
        }
    }
}
