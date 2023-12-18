package org.example;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.ArrayList;
import java.util.List;

public class UserRegistryActor extends AbstractActor {

  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public static class User {
    private final Long id;
    private final String name;
    private final String password;
    private final String email;

    public User(Long id, String name, String password, String email) {
      this.id = id;
      this.name = name;
      this.password = password;
      this.email = email;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getPassword() {
      return password;
    }

    public String getEmail() {
      return email;
    }
  }

  public static class Users{
    private final List<User> users;

    public Users() {
      this.users = new ArrayList<>();
    }

    public Users(List<User> users) {
      this.users = users;
    }

    public List<User> getUsers() {
      return users;
    }
  }

  static Props props() {
    return Props.create(UserRegistryActor.class);
  }

  private final List<User> users = new ArrayList<>();

  @Override
  public Receive createReceive(){
    return receiveBuilder()
            .match(UserRegistryMessages.GetUsers.class, getUsers -> getSender().tell(new Users(users),getSelf()))
            .match(UserRegistryMessages.CreateUser.class, createUser -> {
              users.add(createUser.getUser());
              getSender().tell(new UserRegistryMessages.ActionPerformed(
                      String.format("User %s created.", createUser.getUser().getName())),getSelf());
            })
            .match(UserRegistryMessages.GetUser.class, getUser -> {
              getSender().tell(users.stream()
                      .filter(user -> user.getName().equals(getUser.getName()))
                      .findFirst(), getSelf());
            })
            .match(UserRegistryMessages.DeleteUser.class, deleteUser -> {
              users.removeIf(user -> user.getName().equals(deleteUser.getName()));
              getSender().tell(new UserRegistryMessages.ActionPerformed(String.format("User %s deleted.", deleteUser.getName())),
                      getSelf());

            }).matchAny(o -> log.info("received unknown message"))
            .build();
  }
}
