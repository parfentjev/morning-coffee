package ee.fakeplastictrees.morning_coffee;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class Server {
  private final ExecutorService executor;
  private final Repository repository;

  public Server(ExecutorService executor, Repository repository) {
    this.executor = executor;
    this.repository = repository;
  }

  public void run() throws IOException {
    var server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", new Handler(repository));
    server.setExecutor(executor);
    server.start();
  }

  public static class Handler implements HttpHandler {
    private final Repository repository;

    Handler(Repository repostiry) {
      this.repository = repostiry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      var builder = new StringBuilder();
      builder.append("<h1>Entries</h1><ul>");
      repository.getEntries(10).forEach(entry -> builder.append("<li><a href=\"")
          .append(entry.url())
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
