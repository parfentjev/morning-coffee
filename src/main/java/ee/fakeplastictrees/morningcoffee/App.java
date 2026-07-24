package ee.fakeplastictrees.morningcoffee;

import ee.fakeplastictrees.morningcoffee.reader.Reader;
import java.util.concurrent.Executors;

public class App {
  public static void main(String[] args) throws Exception {
    var config = new Config();
    var repository = new Repository(config.repository());

    var readerExecutor = Executors.newSingleThreadScheduledExecutor();
    var reader = new Reader(config.reader(), readerExecutor, repository);
    reader.run();

    var requestExecutor = Executors.newVirtualThreadPerTaskExecutor();
    var server = new Server(requestExecutor, repository);
    server.run();
  }
}
