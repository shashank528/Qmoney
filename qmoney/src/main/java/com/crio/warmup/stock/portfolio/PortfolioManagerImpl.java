
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.Callable;

public class PortfolioManagerImpl implements PortfolioManager {




  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;
  

// Caution: Do not delete or modify the constructor, or else your build will
  // break! 
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }
 

  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException{

      List<AnnualizedReturn> res = new ArrayList<AnnualizedReturn>();
    try{    
      for(PortfolioTrade i: portfolioTrades)
      { 
        LocalDate currentDate = i.getPurchaseDate();
        List<Candle> candles = getStockQuote(i.getSymbol(), currentDate, endDate);

        Double buyPrice = candles.get(0).getOpen();
        Double sellPrice = candles.get(candles.size()-1).getClose();
      
        
        Double noOfYears = ChronoUnit.DAYS.between(currentDate,endDate) / 365.24;
  
        Double totalReturns = (Double)(sellPrice - buyPrice) / (Double)buyPrice;
        Double annualizedReturns = Math.pow( (1 + totalReturns) , (1 / noOfYears) ) - 1;
  
        res.add( new AnnualizedReturn(i.getSymbol(), annualizedReturns, totalReturns) );

        }
      }catch (JsonProcessingException e) {
        throw new RuntimeException();
      }
        Comparator<AnnualizedReturn> res2 = getComparator();

        Collections.sort(res, res2);

        return res;
           
        
  }

  public List<Candle> getStockQuote(String symbol, LocalDate currentDate, LocalDate endDate)
      throws StockQuoteServiceException, JsonProcessingException{
    return stockQuotesService.getStockQuote(symbol, currentDate, endDate);
    
  }

/*
  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {

        if(from.compareTo(to)>=0){
          throw new RuntimeException();
        }

      List<Candle> ans = new ArrayList<Candle>();
        
      String url = buildUri(symbol, from, to);
      TiingoCandle[] response = restTemplate.getForObject(url,TiingoCandle[].class);
        if(response!=null)
        { ans = Arrays.asList(response); }
       
     return ans;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate =  "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token=af1be48ce97f4d7e1add6f810040ff0e70b6844c";
      return uriTemplate;
    }
    
*/


  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
    LocalDate endDate, int numThreads) throws InterruptedException,StockQuoteServiceException{

    
      List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
      List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
      final ExecutorService pool = Executors.newFixedThreadPool(numThreads);

      for (int i = 0; i < portfolioTrades.size(); i++) {
        PortfolioTrade trade = portfolioTrades.get(i);
        Callable<AnnualizedReturn> callableTask = () -> {
          return getAnnualizedAndTotalReturns(trade, endDate);
        };
        Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
        futureReturnsList.add(futureReturns);
      }
    
      for (int i = 0; i < portfolioTrades.size(); i++) {
        Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
        try {
          AnnualizedReturn returns = futureReturns.get();
          annualizedReturns.add(returns);
        } catch (ExecutionException e) {
          throw new StockQuoteServiceException("Error when calling the API", e);
    
        }
      }
      // Collections.sort(annualizedReturns, Collections.reverseOrder());
      Comparator<AnnualizedReturn> res2 = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
      Collections.sort(annualizedReturns, res2);
      return annualizedReturns;
    }
    
  public AnnualizedReturn getAnnualizedAndTotalReturns(PortfolioTrade trade, LocalDate endDate)
    throws StockQuoteServiceException {
  LocalDate startDate = trade.getPurchaseDate();
  String symbol = trade.getSymbol(); 
 
  Double buyPrice = 0.0, sellPrice = 0.0;
 
  try {
    LocalDate startLocalDate = trade.getPurchaseDate();
    List<Candle> stocksStartToEndFull = getStockQuote(symbol, startLocalDate, endDate);
 
    Collections.sort(stocksStartToEndFull, (candle1, candle2) -> { 
      return candle1.getDate().compareTo(candle2.getDate()); 
    });
    
    Candle stockStartDate = stocksStartToEndFull.get(0);
    Candle stocksLatest = stocksStartToEndFull.get(stocksStartToEndFull.size() - 1);
 
    buyPrice = stockStartDate.getOpen();
    sellPrice = stocksLatest.getClose();
    endDate = stocksLatest.getDate();
 
  } catch (JsonProcessingException e) {
    throw new RuntimeException();
  }
  Double totalReturn = (sellPrice - buyPrice) / buyPrice;
 
  long daysBetweenPurchaseAndSelling = ChronoUnit.DAYS.between(startDate, endDate);
  Double totalYears = (double) (daysBetweenPurchaseAndSelling) / 365;
 
  Double annualizedReturn = Math.pow((1 + totalReturn), (1 / totalYears)) - 1;
  return new AnnualizedReturn(symbol, annualizedReturn, totalReturn);
 
}

  
    
    
    


  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.

}
