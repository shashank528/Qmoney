
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {


  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
  throws JsonProcessingException, StockQuoteServiceException, RuntimeException {
    // TODO Auto-generated method stub

    // if(from.compareTo(to)>=0){
    //   throw new RuntimeException();
    // }

  List<Candle> ans = new ArrayList<Candle>();
    
  String url = buildUri(symbol, from, to);
  // TiingoCandle[] response = restTemplate.getForObject(url,TiingoCandle[].class);
  //   if(response!=null)
  //   { ans = Arrays.asList(response); }
  //   else{
  //     ans = Arrays.asList(new TiingoCandle[0]);
  //   }
try{
  String stocks = restTemplate.getForObject(url, String.class);
  ObjectMapper obj = getObjectMapper();

  TiingoCandle[] response = obj.readValue(stocks,TiingoCandle[].class);

  if(response!=null){
    ans = Arrays.asList(response);
  }
  else{
    ans = Arrays.asList(new TiingoCandle[0]);
  } 

  }catch(NullPointerException e){
    throw new StockQuoteServiceException("Error return while using Tiingo Api",e.getCause());
  }
   
 return ans;
    
}

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate =  "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token=af1be48ce97f4d7e1add6f810040ff0e70b6844c";
   return uriTemplate;
 }




  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF


}
