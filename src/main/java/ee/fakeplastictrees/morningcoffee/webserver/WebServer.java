package ee.fakeplastictrees.morningcoffee.webserver;

import com.sun.net.httpserver.HttpServer;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebServer {
  private final Logger logger = LogManager.getLogger();

  private final Controller controller;
  private final ExecutorService requestExecutor;

  private HttpServer server;

  public WebServer(Repository repository) {
    this.controller = new Controller(repository);
    this.requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public void start() throws IOException {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

    server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", controller.indexHandler());
    // todo: add static files handler
    // (or just put everything into html? ugly)
    server.setExecutor(requestExecutor);
    server.start();

    logger.info("ready to handle requests");
  }

  private void stop() {
    logger.info("shutting down");
    if (server != null) {
      server.stop(5);
    }

    requestExecutor.shutdownNow();
  }
}
