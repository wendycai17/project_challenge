package project;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * This class is used to continuously random uniform distributed generate one or more steams of orders. 
 * The data source generator will first be initialized from main driver, it can run in single or multi
 * thread mode. the main driver will start the order generation process, after data source generate the 
 * orders it will send each order to inventory allocator. This generator will be terminated when all the
 * products in inventor reached zero.
 * <p>
 * 
 * @author wenling
 *
 */
public class DataSourceGenerator {
	private final static String PRODUCTS="ABCDE";
	private final static int MAX_QUANTITY = 5;
    private ExecutorService exec = null;
    private int numStreams = 0;
    private CountDownLatch countDownLatch = null;
    private static final Logger logger = Logger.getLogger(DataSourceGenerator.class.getName());
    
    /**
     * Constructor of DataSourceGenerator
     * it started the thread pool depending on user wants single thread or multi
     * threads. the numStreams by this design is the fixed thread pool size. if 
     * it is one, single thread model will be created. The count down latch is used 
     * to control shutdown executor service.
     * <p>
     * @param numStreams  number of stream from user input. 
     * @param latch   the countdown latch to control the termination
     */
	DataSourceGenerator(int numStreams, CountDownLatch latch){
		this.numStreams = numStreams;
		countDownLatch = latch;
		if(numStreams >1){
			logger.log(Level.INFO, "Use fixed thread pool");
			exec = Executors.newFixedThreadPool(numStreams);
		}
		else if(numStreams == 1){
			logger.log(Level.INFO, "Use single thread");
			exec = Executors.newSingleThreadExecutor();
		}
		else{
			assert(false);
		}
	}
		
	/**
	 * This method will be invoked from main driver to start generating orders, it will
	 * call generateOrder which will generate random uniform distributed orders.
	 * @see generateOrder
	 * <p>
	 */
	public void start(){
		for (int i= 0; i < numStreams; ++i){
			//each thread is a stream which has a uniq id as stream id
			String streamId = UUID.randomUUID().toString();
			Runnable task = new Runnable(){
				
				public void run(){
					    final Random quantityRandom = new Random(MAX_QUANTITY);
						int iHeaderId = 1;
					    while(!Thread.currentThread().isInterrupted() && countDownLatch.getCount() != 0){
					    	//generate an order
					    	logger.log(Level.INFO, "Generate order for stream header " + streamId + ":" + iHeaderId);
					    	generateOrder(streamId, String.valueOf(iHeaderId), quantityRandom);
					    	iHeaderId ++;
					    	try {
					    	   Thread.sleep(5000);
					    	} catch (InterruptedException e) {
					    		Thread.currentThread().interrupt();
					    	}
					    }
					}
				
			};
		    exec.submit(task);
		}
	}
	
	/**
	 * This method is invoked from main driver when run out of inventory, the count down latch is waked. 
	 * it will stop the all the running threads and shut down the executor service.
	 * <p> 
	 */
	public void stop(){
		if(exec != null){
			exec.shutdown();
		}
	}

    /**
     * This is the main code to random generate an single order, After the order is generated, 
     * it will publish the order to inventory allocator for inventory management based on the requirement
     * for this project
     * <p>
     * 
     * @param streamId  the stream id passed from user
     * @param headerId  the generated header id
     * @param quantityRandom  the product quantity Random sequence
     */
	private void generateOrder(String streamId, String headerId, Random quantityRandom){
	    int numLines = PRODUCTS.length();
		int quantity = 0;
	    String productName = null;
		int sumQuantity = 0;
		ArrayList<OrderLine> orderLines = new ArrayList<OrderLine>();
		Order order = null;
		// create the order lines for a single order. currently only quantity is random generated, 
		// the number of lines in always five, it can also be random generated. In the case when one of
		// product is zero, an order line will be created. 
		for(int i = 0; i < numLines; ++i){
			productName = String.valueOf(PRODUCTS.charAt(i));
			quantity = quantityRandom.nextInt(5);
			OrderLine oLine = new OrderLine(productName, quantity);
			orderLines.add(oLine);
		    sumQuantity =+ quantity;
			
		}
		// if quantity of all order lines are zero, dropped the order since it's not a valid order.
		if (sumQuantity != 0){
			order = new Order(streamId + ":" + headerId, orderLines);
			logger.log(Level.INFO, "headerId is:" + streamId + ":" + headerId);
			for(OrderLine line: orderLines){
				logger.log(Level.INFO, "product name is:" + line.getProductName() + " quantity is: " + line.getProductQuantity());
			}
		
			InventoryAllocator ia = InventoryAllocator.getInventoryAllocator(countDownLatch);
			ia.processOrder(order);
		}
	}

}



			
	
	

