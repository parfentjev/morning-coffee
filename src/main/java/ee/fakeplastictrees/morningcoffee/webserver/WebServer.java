package ee.fakeplastictrees.morningcoffee.webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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

  private final ExecutorService requestExecutor;
  private final Repository repository;

  private HttpServer server;

  public WebServer(Repository repository) {
    this.repository = repository;
    this.requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public void start() throws IOException {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));

    server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", new Handler(repository));
    server.setExecutor(requestExecutor);
    server.start();

    logger.info("server is ready to handle requests: http://0.0.0.0:8080/");
  }

  private void stop() {
    logger.info("shutting down");
    if (server != null) {
      server.stop(5);
    }

    requestExecutor.shutdownNow();
  }

  private static class Handler implements HttpHandler {
    private final Repository repository;

    Handler(Repository repostiry) {
      this.repository = repostiry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var builder = new StringBuilder();
      builder.append("<h1>Entries</h1><ul>");
      repository
          .getEntries(10)
          .forEach(
              entry ->
                  builder
                      .append("<li><a href=\"")
                      .append(entry.link())
                      .append("\" target=\"_blank\">")
                      .append(entry.title())
                      .append("</a></li>"));
      builder.append("</ul>");

      var body = builder.toString().getBytes();
      exchange.sendResponseHeaders(200, body.length);
      try (var outputStream = exchange.getResponseBody()) {
        outputStream.write(body);
      }
    }
  }
}
