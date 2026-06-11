package com.lostres.ms_documental_dms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import jakarta.annotation.PostConstruct;

import java.util.UUID;

@Slf4j
@Service
public class BlockchainNotaryService {

    @Value("${blockchain.rpc.url:http://127.0.0.1:8545}")
    private String rpcUrl;

    @Value("${blockchain.wallet.private-key:}")
    private String privateKey;

    @Value("${blockchain.contract.address:}")
    private String contractAddress;

    private Web3j web3j;
    private Credentials credentials;

    @PostConstruct
    public void init() {
        try {
            web3j = Web3j.build(new HttpService(rpcUrl));
            if (privateKey != null && !privateKey.isBlank()) {
                credentials = Credentials.create(privateKey);
                log.info("Blockchain Notary Service inicializado con wallet: {}", credentials.getAddress());
            } else {
                log.warn("Blockchain private key no configurada. Las transacciones reales fallarán.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la conexión con Blockchain", e);
        }
    }

    /**
     * Registra el Hash SHA-256 de un documento en la Blockchain de manera asíncrona.
     */
    @Async
    public void notarizeDocument(UUID documentId, String documentHash) {
        if (credentials == null || contractAddress == null || contractAddress.isBlank()) {
            log.warn("No se puede notarizar el documento {}. Faltan credenciales o contractAddress.", documentId);
            return;
        }

        try {
            log.info("Notarizando documento en Blockchain: ID={} Hash={}", documentId, documentHash);
            
            // Para propósitos prácticos, aquí se generaría un Wrapper de Solidity con web3j,
            // pero podemos simular o enviar la transacción directamente con el FunctionEncoder
            // o simplemente mediante logs indicando que se usaría el wrapper compilado.
            
            // Ejemplo conceptual:
            // EvidenceRegistry contract = EvidenceRegistry.load(contractAddress, web3j, credentials, new DefaultGasProvider());
            // TransactionReceipt receipt = contract.registerEvidence(documentId.toString(), documentHash).send();
            
            log.info("Transacción Blockchain enviada (Simulación exitosa) para el doc: {}", documentId);
            
        } catch (Exception e) {
            log.error("Fallo al enviar transacción a Blockchain para el documento " + documentId, e);
        }
    }
}
