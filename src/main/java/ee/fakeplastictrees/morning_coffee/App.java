package ee.fakeplastictrees.morning_coffee;

import java.util.concurrent.Executors;

public class App {
  public static void main(String[] args) throws Exception {
    var config = new Config();

    var repository = new Repository();

    var readerExecutor = Executors.newSingleThreadScheduledExecutor();
    var reader = new Reader(readerExecutor, repository);
    reader.run();

    var requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
    var server = new Server(requestExecutor, repository);
    server.run();
  }
}
