package org.example;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

public class StartServer extends AllDirectives {

	private final UserRoutes userRoutes;

	public StartServer(ActorSystem system, ActorRef userRegistryActor) {
		userRoutes = new UserRoutes(system, userRegistryActor);
	}

	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("helloAkkaHttpServer");

		final Http http = Http.get(system);
		final ActorMaterializer materializer = ActorMaterializer.create(system);

		ActorRef userRegistryActor = system.actorOf(UserRegistryActor.props(), "userRegistryActor");

		StartServer app = new StartServer(system, userRegistryActor);

		final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute()
				.flow(system, materializer);

		http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);

		System.out.println("Server online at http://localhost:8080/");

	}
	protected Route createRoute() {
		return userRoutes.routes();
	}
}


