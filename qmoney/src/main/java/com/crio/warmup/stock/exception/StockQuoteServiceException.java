
package com.crio.warmup.stock.exception;

public class StockQuoteServiceException extends Exception {

  public StockQuoteServiceException(String message) {
    super(message);
    System.out.println("shashankl");
  }

  public StockQuoteServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
