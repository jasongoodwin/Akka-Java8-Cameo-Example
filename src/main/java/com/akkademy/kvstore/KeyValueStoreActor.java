package com.akkademy.kvstore;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class KeyValueStoreActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    Map<String, Object> store = new HashMap<>();

    public KeyValueStoreActor() {
        receive(ReceiveBuilder.
                        match(Get.class, msg -> {
                            GetResponse response = new GetResponse(msg.getKey(), store.get(msg.getKey()));
                            log.info("Store returning: " + response);
                            sender().tell(response, self());
                        }).
                        match(Set.class, msg -> {
                            store.put(msg.getKey(), msg.getValue());
                            sender().tell(AckSet.get(), self());
                        }).
                        matchAny(o -> log.info("received unknown message: " + o.getClass())).build()
        );
    }

    public static Props props(){
        return Props.create(KeyValueStoreActor.class);
    }
}
