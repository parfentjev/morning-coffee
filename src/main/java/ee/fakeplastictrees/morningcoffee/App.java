package ee.fakeplastictrees.morningcoffee;

import ee.fakeplastictrees.morningcoffee.reader.ScheduledFeedReader;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.WebServer;
import java.io.IOException;

public class App {
  // todo: remove IOException
  public static void main(String[] args) throws IOException {
    var config = new Config();
    var repository = new Repository(config.repository());

    var reader = new ScheduledFeedReader(config.reader(), repository);
    reader.start();

    var server = new WebServer(repository);
    server.start();
  }
}
