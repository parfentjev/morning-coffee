package ee.fakeplastictrees.morning_coffee;

import java.util.List;
import java.util.concurrent.Executors;

public class App {
  public static void main(String[] args) throws Exception {
    var repository = new Repository();

    var readerExecutor = Executors.newSingleThreadScheduledExecutor();
    var feedList = List.of("feed1", "feed2", "feed3");
    var reader = new Reader(readerExecutor, repository, feedList);
    reader.run();

    var requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
    var server = new Server(requestExecutor, repository);
    server.run();
  }
}
