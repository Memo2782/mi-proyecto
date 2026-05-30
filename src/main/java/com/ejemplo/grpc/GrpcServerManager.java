package com.ejemplo.grpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.ejemplo.grpc.OrderRequest;
import com.ejemplo.grpc.OrderResponse;
import com.ejemplo.grpc.OrderServiceGrpc;
import com.ejemplo.akka.OrderActor;
import com.ejemplo.repository.OrderRepository;
import com.ejemplo.smpp.SmppClientService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Component
public class GrpcServerManager extends OrderServiceGrpc.OrderServiceImplBase {
    private static final Logger logger = LogManager.getLogger(GrpcServerManager.class);

    private final OrderRepository repository;
    private final SmppClientService smppService;
    private final MeterRegistry meterRegistry;
    
    private Server server;
    private ActorSystem actorSystem;
    private ActorRef orderActor;

    public GrpcServerManager(OrderRepository repository, SmppClientService smppService, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.smppService = smppService;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void start() throws IOException {
        this.actorSystem = ActorSystem.create("OrderActorSystem");
        this.orderActor = actorSystem.actorOf(OrderActor.props(repository, smppService, meterRegistry), "orderActor");

        this.server = ServerBuilder.forPort(50051)
                .addService(this)
                .build()
                .start();
        logger.info("Servidor gRPC iniciado de forma nativa en el puerto 50051");
    }

    @Override
    public void insertOrder(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
        logger.info("Mensaje gRPC delegado al sistema de Actores para ID: {}", request.getOrderId());
        orderActor.tell(new OrderActor.ProcessOrderMsg(request, responseObserver), ActorRef.noSender());
    }

    @PreDestroy
    public void stop() {
        if (server != null) { server.shutdown(); }
        if (actorSystem != null) { actorSystem.terminate(); }
    }
}
