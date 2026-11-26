package ee.fakeplastictrees.morningcoffee.brief;

class BriefServiceException extends Exception {
  BriefServiceException(String message) {
    super(message);
  }

  BriefServiceException(String message, Exception cause) {
    super(message, cause);
  }
}
