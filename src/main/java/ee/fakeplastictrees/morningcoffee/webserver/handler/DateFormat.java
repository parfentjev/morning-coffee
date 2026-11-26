package ee.fakeplastictrees.morningcoffee.webserver.handler;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class DateFormat {
  static final DateTimeFormatter FULL = DateTimeFormatter.ISO_INSTANT;
  static final DateTimeFormatter SHORT =
      DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneOffset.UTC);
}
