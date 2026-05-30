package com.ejemplo.smpp;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;

@Service
public class SmppClientService {
    private static final Logger logger = LogManager.getLogger(SmppClientService.class);
    private SmppSession smppSession;

    @PostConstruct
    public void init() {
        DefaultSmppClient client = new DefaultSmppClient(Executors.newCachedThreadPool(), 1);
        SmppSessionConfiguration config = new SmppSessionConfiguration();
        config.setWindowSize(1);
        config.setName("SMS.Client");
        config.setType(SmppBindType.TRANSCEIVER);
        config.setHost("127.0.0.1");
        config.setPort(2775);
        config.setSystemId("user");
        config.setPassword("pass");
        logger.info("Cliente SMPP inicializado de forma programática.");
    }

    public void sendSms(String toAddress, String text) {
        if (smppSession == null || !smppSession.isBound()) {
            logger.warn("[Simulación SMS] Para: {} | Texto: {}", toAddress, text);
            return;
        }
        try {
            SubmitSm sms = new SubmitSm();
            sms.setSourceAddress(new Address((byte)0, (byte)0, "API_GATEWAY"));
            sms.setDestAddress(new Address((byte)0, (byte)0, toAddress));
            sms.setShortMessage(text.getBytes("UTF-8"));
            smppSession.submit(sms, 4000);
            logger.info("SMS despachado vía SMPP.");
        } catch (Exception e) {
            logger.error("Error al enviar el paquete de datos SMPP", e);
        }
    }
}
