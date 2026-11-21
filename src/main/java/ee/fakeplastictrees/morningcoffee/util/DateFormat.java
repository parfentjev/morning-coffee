package ee.fakeplastictrees.morningcoffee.util;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateFormat {
  public static final DateTimeFormatter FULL = DateTimeFormatter.ISO_INSTANT;
  public static final DateTimeFormatter SHORT =
      DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneOffset.UTC);
}
