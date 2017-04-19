package project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the inventory allocator, it received the order from data source generator
 * it picked the order based on the first in, first order. Once the inventory is allocated, 
 * it can not assigned to other order. If there is no enough inventory for the order line, 
 * a backorder will be recorded. If the line is filled, a filled order will be recorded. 
 * when all the products inventory is reached 0, it triggered count down latch to awake and 
 * all the running threads, service will smooth shut down. 
 * 
 * This class can be in single thread or multi threads case depends on the user setting for how 
 * many threads it will run. Currently it is hard coded as 5, user can use a configuration file 
 * to specify and program can load the setting from property file.
 * in the single case, inventory allocation is serialized. In the multi threads case, the order 
 * is pick on first come first serve, the inventory allocation is safe. The finished process order
 * is not serialized.
 * <p>
 * @author wenling
 *
 */
public class InventoryAllocator{
	private static InventoryAllocator instance = null;
	private static final int NUM_THREADS = 5;
	private CountDownLatch gateLatch = null;
	private ExecutorService executor = null;
	private HashMap<String, SafeInteger> inventoryMap = new HashMap<String, SafeInteger>();
	private Map<String, OrderStatus> orderTable = 
			Collections.synchronizedMap(new LinkedHashMap<String, OrderStatus>());
	volatile boolean shouldStop = false;
	volatile boolean shouldPrint = true;
	private static final Logger logger = Logger.getLogger(InventoryAllocator.class.getName());
	private static final Object syncObj = new Object();
	
	//sum of the initial invertory counts for all products.
	private static SafeInteger sumCount = null;
	/**
	 * Constructor of the inventory allocator, it's a singleton class
	 * 
	 * @param nThreads  number of threads will be created in the thread pool.
	 * @param latch     count down latch to control the termination when all inventory reach 0
	 */
	private InventoryAllocator(int nThreads, CountDownLatch latch){
		logger.log(Level.INFO, "Enter InventoryAllocator");
		executor = Executors.newFixedThreadPool(nThreads);
	    inventoryMap.put("A", new SafeInteger(10));
		inventoryMap.put("B", new SafeInteger(10));
		inventoryMap.put("C", new SafeInteger(15));
		inventoryMap.put("D", new SafeInteger(15));
		inventoryMap.put("E", new SafeInteger(20));
		sumCount = new SafeInteger(60); // sum of the inventories above
		gateLatch = latch;
	}
	
	/**
	 * Trigged from main driver to shut down the executor service.
	 */
	public void stop() {
		executor.shutdown();
	}
	
	/**
	 * This method client can use to get the singleton instance.
	 * <p>
	 * @param latch
	 * @return the singleton instance of the inventory allocator
	 */
	public synchronized static InventoryAllocator getInventoryAllocator(CountDownLatch latch){
		if(instance == null)
			instance = new InventoryAllocator(NUM_THREADS, latch);
		return instance;
		
	}
	/**
	 * Method will be called from DataSourceGenerator when the order is ready
	 * for inventory allocator.
	 * <p>
	 * @param order a valid order
	 */
	public void processOrder(Order order){
		logger.log(Level.INFO, "Processing order");
		executor.submit(new OrderProcessor(order));
	}
	/**
	 * print out the final results for the inventory allocation
	 */
	public synchronized void printOrders(){
		for(String key: orderTable.keySet()){
			OrderStatus oStatus = orderTable.get(key);
			ArrayList<OrderLine> oLines = oStatus.getOrderLine();
			ArrayList<OrderLine> fLines = oStatus.getFilledOrder();
			
			int numLines = oLines.size();
			//logger.log(Level.WARNING, "key is " + key + "order line size: " + numLines + " filled line size" + fLines.size());
			int ordered[] = new int[numLines];
			int filled[] = new int[fLines.size()];
			int backed[] = new int[numLines];
			int oQuantity, fQuantity = 0;
			for(int i =0, j = 0, k=0; i < oLines.size() && j < fLines.size(); ++i, ++j){
				oQuantity = oLines.get(i).getProductQuantity();
				fQuantity = fLines.get(j).getProductQuantity();
				if(oLines.get(i).getProductName().equals(fLines.get(j).getProductName())){
					ordered[k] = oQuantity;
					filled[k] = fQuantity;
					backed[k]= oQuantity - fQuantity;
					k++;
				}
			}
			//print out the output for each record
			
			System.out.print(key + "  ");
			for(int x = 0; x < numLines; ++x){
				System.out.print(ordered[x] + " ");
			}
			System.out.print(" ");
			for(int y = 0; y < numLines; ++y){
				System.out.print(filled[y] + " ");
			}
			System.out.print(" ");
			for(int z = 0; z < numLines; ++z){
				System.out.print(backed[z] + " ");
			}
			System.out.println();
			
	
		}
		gateLatch.countDown();
	}
	
	/**
	 * 
	 * the callable class which perform the inventory allocation, make sure the inventory
	 * allocation meets the requirement of this project.
	 * It received order, for each order check against inventory to make sure if there is 
	 * enough inventory for the line, the order is filled, if there is no enough inventory for 
	 * line, back order is kept tracked. Inventory access is thread safe.
	 * @see InventoryAllocator for requirement.
	 *
	 */
	class OrderProcessor implements Callable<Boolean>{
		Order order;
		OrderProcessor(Order order){
			this.order = order;
		}
		
		public Boolean call(){
			logger.log(Level.INFO, "Enter OrderProcess call()");
			String headId = order.getHeadId();
			logger.log(Level.INFO, "get order headId:" + headId);
			ArrayList<OrderLine> orderLines = order.getOrderLines();
			String productName = null;
			int productQuantity = 0;
	
			ArrayList<OrderLine> filledOrder =  new ArrayList<OrderLine>();
			logger.log(Level.INFO, "received number of order lines:" + orderLines.size());
		    if(shouldStop)
		    	return shouldStop;
			for(OrderLine line: orderLines){
				productName = line.getProductName();
				productQuantity = line.getProductQuantity();
				logger.log(Level.FINEST, "Rev ProductName is " + productName + " Quantity is " + productQuantity);
				SafeInteger i = inventoryMap.get(productName);
				
				if (i.decreaseBy(productQuantity)){
					filledOrder.add(new OrderLine(productName, productQuantity));
         		}
				else{
					filledOrder.add(new OrderLine(productName, 0));
				}
								
			}
			OrderStatus processedOrder = new OrderStatus(orderLines, filledOrder);
			orderTable.put(headId, processedOrder);
			if(isAllZero()) {
				shouldStop = new Boolean(true);
			}
			logger.log(Level.FINEST, "Is all zero " + shouldStop);
			if(shouldStop && shouldPrint){
				shouldPrint = false;
				printOrders();
			}
			return shouldStop;
		}
		
		boolean isAllZero(){
			for(String key: inventoryMap.keySet()){
				if (!inventoryMap.get(key).isZero()) {
					return false;
				}
			}
			return true;
		}
	}	
}
