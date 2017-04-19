package project;

import java.util.concurrent.CountDownLatch;
import java.util.logging.*;

/**
 * Main driver for this project
 * instantiate the countdown latch
 * start the data source generator
 * wait on the latch
 * stop the data generator and inventory allocator.
 * <p>
 * @author wenling
 *
 */
public class MainDriver {
	private static final Logger logger = Logger.getLogger(MainDriver.class.getName());
	public static void main(String[] args) {
		
		//instantiate latch
		final CountDownLatch gateLatch = new CountDownLatch(1);
		//start datasource generator
		DataSourceGenerator dsg = new DataSourceGenerator(3,gateLatch);
		
		dsg.start();
		//wait on latch
		try{
			gateLatch.await();
			logger.log(Level.INFO, "latch awaked");
			//stop the data source generator and inventory allocator
			dsg.stop();
			InventoryAllocator.getInventoryAllocator(gateLatch).stop();
		}
		catch(InterruptedException ie){
			logger.log(Level.SEVERE, "Interrupted", ie);
			
		}

	}

}
