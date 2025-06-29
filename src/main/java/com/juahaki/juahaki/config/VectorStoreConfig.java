package com.juahaki.juahaki.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.use-tls}")
    private boolean useTls;

    @Bean
    @Primary
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client with host: {}, port: {}, TLS: {}",
                qdrantHost, qdrantPort, useTls);
        try {
            QdrantGrpcClient.Builder clientBuilder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, useTls)
                    .withApiKey(qdrantApiKey);

            QdrantGrpcClient grpcClient = clientBuilder.build();
            QdrantClient client = new QdrantClient(grpcClient);

            return client;
        } catch (Exception e) {
            log.error("Failed to initialize qdrant client");
            throw new RuntimeException("Failed to connect to Qdrant: " + e.getMessage(), e);

        }
    }

    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {

        log.info("Initializing Qdrant vector store");

        try {

            return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                    .collectionName(collectionName)
                    .initializeSchema(true)
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize vector store: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize vector store: " + e.getMessage(), e);
        }
    }
}
