package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import org.example.UserRegistryActor.User;
import org.example.UserRegistryMessages.ActionPerformed;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class UserRoutes extends AllDirectives {
    final private ActorRef userRegistryActor;
    final private LoggingAdapter log;


    public UserRoutes(ActorSystem system, ActorRef userRegistryActor) {
        this.userRegistryActor = userRegistryActor;
        log = Logging.getLogger(system, this);
    }

    Duration timeout = Duration.ofSeconds(5L);

    public Route routes() {
        return route(pathPrefix("users", () ->
            route(
                getOrPostUsers(),
                path(PathMatchers.segment(), name -> route(
                    getUser(name),
                    deleteUser(name)
                  )
                )
            )
        ));
    }
    private Route getUser(String name) {
      return get(() -> {

          CompletionStage<Optional> maybeUser = Patterns
                  .ask(userRegistryActor, new UserRegistryMessages.GetUser(name), timeout)
                  .thenApply(Optional.class::cast);

          return onSuccess(() -> maybeUser,
              performed -> {
                  if (performed.isPresent())
                      return complete(StatusCodes.OK, performed.get(), Jackson.marshaller());
                  else
                      return complete(StatusCodes.NOT_FOUND);
              }
            );
        });
    }

    private Route deleteUser(String name) {
      return
          delete(() -> {
            CompletionStage<ActionPerformed> userDeleted = Patterns
              .ask(userRegistryActor, new UserRegistryMessages.DeleteUser(name), timeout)
              .thenApply(ActionPerformed.class::cast);

            return onSuccess(() -> userDeleted,
              performed -> {
                log.info("Deleted user [{}]: {}", name, performed.getDescription());
                return complete(StatusCodes.OK, performed, Jackson.marshaller());
              }
            );
          });

    }
    private Route getOrPostUsers() {
        return pathEnd(() ->
            route(
                get(() -> {
                    CompletionStage<UserRegistryActor.Users> futureUsers = Patterns
                        .ask(userRegistryActor, new UserRegistryMessages.GetUsers(), timeout)
                        .thenApply(UserRegistryActor.Users.class::cast);
                    return onSuccess(() -> futureUsers,
                        users -> complete(StatusCodes.OK, users, Jackson.marshaller()));
                }),
                post(() ->
                    entity(
                        Jackson.unmarshaller(User.class),
                        user -> {
                            CompletionStage<ActionPerformed> userCreated = Patterns
                                .ask(userRegistryActor, new UserRegistryMessages.CreateUser(user), timeout)
                                .thenApply(ActionPerformed.class::cast);
                            return onSuccess(() -> userCreated,
                                performed -> {
                                    log.info("Created user [{}]: {}", user.getName(), performed.getDescription());
                                    return complete(StatusCodes.CREATED, performed, Jackson.marshaller());
                                });
                        }))
            )
        );
    }
}
