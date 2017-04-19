package project;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class InventoryTest{
	private static InventoryTest instance = null;
	private CountDownLatch gateLatch = null;
	
	private HashMap<String, SafeInteger> inventoryMap = new HashMap<String, SafeInteger>();
	private LinkedHashMap<String, OrderStatus> orderTable = 
			(LinkedHashMap<String, OrderStatus>) Collections.synchronizedMap(new LinkedHashMap<String, OrderStatus>());
	
	//sum of the initial invertory counts for all products.
	private static SafeInteger sumCount = null;
	/*
	private InventoryTest(int nThreads){
		System.out.println("Enter InventoryAllocator");
		//exec = Executors.newFixedThreadPool(nThreads);
	    inventoryMap.put("A", new SafeInteger(150));
		inventoryMap.put("B", new SafeInteger(150));
		inventoryMap.put("C", new SafeInteger(100));
		inventoryMap.put("D", new SafeInteger(100));
		inventoryMap.put("E", new SafeInteger(200));
		sumCount = new SafeInteger(700); // sum of the inventories above
	}
	*/
	
	public static void test() {
		System.out.println("test");
	}

}
