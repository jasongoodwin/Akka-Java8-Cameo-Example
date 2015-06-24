package com.akkademy;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import com.akkademy.kvstore.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class ApplicationMain {

    public static void main(String[] args) throws Exception{
        ActorSystem system = ActorSystem.create("CameoTestSystem");

        //Setup

        ActorRef kvStoreActor = system.actorOf(KeyValueStoreActor.props(), "kvstore");

        String username = "saravana";

        kvStoreActor.tell(new Set(username + ":email", "saravana@tcs.com"), null);
        kvStoreActor.tell(new Set(username + ":about", "Saravana is a software developer " +
                "currently building cool stuff."), null);
        kvStoreActor.tell(new Set(username + ":interests", Arrays.asList("Java8", "Akka", "Play")), null);

        ActorRef profileService = system.actorOf(ProfileServiceActor.props(kvStoreActor));

        Future f = Patterns.ask(profileService, new GetUserProfile(username), 1000L);
        UserProfile result = (UserProfile) Await.result(f, Duration.create(1, "second"));
        System.out.println("Got the profile: " + result);
        system.awaitTermination();
    }
}

class UserProfile{
    final String username;
    final String email;
    final String about;
    final List<String> interests;

    public UserProfile(String username, String email, String about, List<String> interests) {
        this.username = username;
        this.email = email;
        this.about = about;
        this.interests = interests;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", about='" + about + '\'' +
                ", interests=" + interests +
                '}';
    }
}

class GetUserProfile{
    private final String username;

    public GetUserProfile(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

/**
 * Service Actor. Instead of asking, it creates the cameo actor to collect together the response.
 */

class ProfileServiceActor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    public ProfileServiceActor(ActorRef kvStore) {
        receive(ReceiveBuilder.
                match(GetUserProfile.class, msg -> {
                    log.info("Getting profile for user: " + msg);
                    ActorRef cameo = context().actorOf(ProfileBuilderActor.props(kvStore, msg.getUsername(), sender()));
                    kvStore.tell(new Get(msg.getUsername() + ":email"), cameo);
                    kvStore.tell(new Get(msg.getUsername() + ":about"), cameo);
                    kvStore.tell(new Get(msg.getUsername() + ":interests"), cameo);
                }).
                matchAny(o -> log.info(this.getClass() + " received unknown message: " + o.getClass())).
                build());
    }

    public static Props props(ActorRef kvStore) {
        return Props.create(ProfileServiceActor.class, kvStore);
    }
}

/**
 * Cameo Example
 * Actor builds a user profile
 */
class ProfileBuilderActor  extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    private final ActorRef kvStore;
    private final ActorRef replyTo;
    private final String username;

    private Optional<String> email = Optional.empty();
    private Optional<String> about = Optional.empty();
    private Optional<List<String>> interests = Optional.empty();

    public ProfileBuilderActor(ActorRef kvStore, String username, ActorRef replyTo) {
        this.kvStore = kvStore;
        this.replyTo = replyTo;
        this.username = username;

        scheduleTimeout();

        receive(ReceiveBuilder.
                        match(GetResponse.class, x -> x.getKey().endsWith(":email"), msg -> {
                            email = Optional.of((String) msg.getValue());
                            collectProfile();
                        }).
                        match(GetResponse.class, x -> x.getKey().endsWith(":about"), msg -> {
                            about = Optional.of((String) msg.getValue());
                            collectProfile();
                        }).
                        match(GetResponse.class, x -> x.getKey().endsWith(":interests"), msg -> {
                            interests = Optional.of((List<String>) msg.getValue());
                            collectProfile();
                        }).
                        match(String.class, x -> x.equals("timeout"), msg -> {
                            context().parent().tell(new Status.Failure(new TimeoutException("Timeout building profile")), self());
                            context().stop(self());
                        }).
                        matchAny(o -> log.info(this.getClass() + " received unknown message: " + o.getClass())).
                        build()
        );
    }

    private void scheduleTimeout() {
        Runnable r = () -> self().tell("timeout", null);

        context().system().scheduler().scheduleOnce(
                Duration.create(1, "second"),
                r,
                context().dispatcher());
    }

    public static Props props(ActorRef kvStore, String username, ActorRef replyTo){
        return Props.create(ProfileBuilderActor.class, kvStore, username, replyTo);
    }

    private void collectProfile(){
        if(email.isPresent() && about.isPresent() && interests.isPresent()) {
            UserProfile response = new UserProfile(
                    username,
                    email.get(),
                    about.get(),
                    interests.get()
            );

            replyTo.tell(response, self());
            context().stop(self());
        }
    }
}

