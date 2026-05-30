package com.ejemplo.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;

public class GrpcTestClient {
    private static final Logger logger = LogManager.getLogger(GrpcTestClient.class);

    public static void main(String[] args) {
        logger.info("Estableciendo canal de comunicación gRPC en localhost:50051...");
        
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        try {
            OrderServiceGrpc.OrderServiceBlockingStub stub = OrderServiceGrpc.newBlockingStub(channel);

            OrderRequest request = OrderRequest.newBuilder()
                    .setOrderId("ORD-2026-PERU")
                    .setCustomerId("CUST-999-LIMA")
                    .setCustomerPhoneNumber("+51999888777")
                    .addAllItems(Arrays.asList("MacBook Pro M3", "Audífonos Inalámbricos"))
                    .build();

            logger.info("Enviando estructura de pedido hacia el Servidor gRPC...");
            OrderResponse response = stub.insertOrder(request);
            
            logger.info("¡Respuesta gRPC recibida con éxito!");
            logger.info("ID de Pedido Confirmado: {}", response.getOrderId());
            logger.info("Estado del Flujo: {}", response.getStatus());

        } catch (Exception e) {
            logger.error("Fallo crítico en la comunicación con el canal gRPC: {}", e.getMessage());
        } finally {
            channel.shutdown();
        }
    }
}
