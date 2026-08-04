package ee.fakeplastictrees.morningcoffee.reader;

import java.io.IOException;

/// A request operation executed by `ThrottlingManager` according to its configuration.
interface ThrottableRequest<T> {
  /// Executes the request operation.
  ///
  /// @throws InterruptedException if the thread is interrupted
  /// @throws IOException if an I/O error occurs
  T run() throws InterruptedException, IOException;
}
