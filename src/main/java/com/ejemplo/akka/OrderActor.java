package com.ejemplo.akka;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.ejemplo.grpc.OrderRequest;
import com.ejemplo.grpc.OrderResponse;
import com.ejemplo.model.Order;
import com.ejemplo.repository.OrderRepository;
import com.ejemplo.smpp.SmppClientService;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.OffsetDateTime;

public class OrderActor extends AbstractActor {
    private static final Logger logger = LogManager.getLogger(OrderActor.class);

    private final OrderRepository repository;
    private final SmppClientService smppService;
    private final Counter orderCounter;

    public static class ProcessOrderMsg {
        public final OrderRequest request;
        public final StreamObserver<OrderResponse> responseObserver;

        public ProcessOrderMsg(OrderRequest request, StreamObserver<OrderResponse> responseObserver) {
            this.request = request;
            this.responseObserver = responseObserver;
        }
    }

    public OrderActor(OrderRepository repository, SmppClientService smppService, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.smppService = smppService;
        // Instantiates or fetches the Prometheus metrics counter explicitly on startup
        this.orderCounter = Counter.builder("app.orders.processed.total")
                .description("Contador global de pedidos procesados por Akka")
                .register(meterRegistry);
    }

    public static Props props(OrderRepository repository, SmppClientService smppService, MeterRegistry meterRegistry) {
        return Props.create(OrderActor.class, () -> new OrderActor(repository, smppService, meterRegistry));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProcessOrderMsg.class, msg -> {
                    logger.info("Actor Classic procesando payload para ID: {}", msg.request.getOrderId());
                    
                    Order order = new Order();
                    order.setOrderId(msg.request.getOrderId());
                    order.setCustomerId(msg.request.getCustomerId());
                    order.setCustomerPhoneNumber(msg.request.getCustomerPhoneNumber());
                    order.setItems(msg.request.getItemsList());
                    order.setStatus("PROCESSED");
                    order.setTs(OffsetDateTime.now());

                    repository.save(order).subscribe(
                        savedOrder -> {
                            logger.info("Guardado confirmado en MongoDB: {}", savedOrder.getOrderId());
                            
                            String smsText = "Your order " + msg.request.getOrderId() + " has been processed";
                            smppService.sendSms(savedOrder.getCustomerPhoneNumber(), smsText);

                            OrderResponse response = OrderResponse.newBuilder()
                                    .setOrderId(savedOrder.getOrderId())
                                    .setStatus("SUCCESS")
                                    .build();
                            
                            msg.responseObserver.onNext(response);
                            msg.responseObserver.onCompleted();

                            orderCounter.increment();
                        },
                        error -> {
                            logger.error("Error en la tubería reactiva de persistencia", error);
                            msg.responseObserver.onError(error);
                        }
                    );
                })
                .build();
    }
}
