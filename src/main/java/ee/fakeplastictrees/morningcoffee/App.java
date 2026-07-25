package ee.fakeplastictrees.morningcoffee;

import ee.fakeplastictrees.morningcoffee.reader.ScheduledFeedReader;
import ee.fakeplastictrees.morningcoffee.repository.Repository;
import ee.fakeplastictrees.morningcoffee.webserver.WebServer;

public class App {
  public static void main(String[] args) throws Exception {
    var config = new Config();
    var repository = new Repository(config.repository());

    var reader = new ScheduledFeedReader(config.reader(), repository);
    reader.start();

    var server = new WebServer(repository);
    server.start();
  }
}
