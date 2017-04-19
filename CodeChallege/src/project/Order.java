package project;
import java.util.ArrayList;

/**
 * This file contains several utility classes to build the basic structure for 
 * this project. Most of the classes are designed as immutable class for thread 
 * safe purpose.
 * <p>
 * 
 * @author wenling
 *
 */

/**
 * 
 * This class represents a valid order. it is effective thread safe under the use 
 * scope of this project.
 * 
 * An order consists of a unique identifier (per stream) we will call the "header", 
 * and a demand for between zero and five units each of A, B, C, D, and E, 
 * except that there must be at least one unit demanded. 
 *
 */
public class Order{
	private final String headId;
	private final ArrayList<OrderLine> OrderLines;
	
	Order(String headId, ArrayList<OrderLine> lines){
		this.headId = headId;
	    this.OrderLines = lines;
	}
	
	public String getHeadId(){
		return headId;
	}
	
	public ArrayList<OrderLine> getOrderLines(){
		return OrderLines;
	}
}

/**
 * This class represent each order line, each order line contains product name and 
 * quantity of the associated product.
 * <p>
 */
class OrderLine{
	private final String productName;
	private final int quantity;
	
	OrderLine(String productName, int quantity){
		this.productName = productName;
		this.quantity = quantity;
	}
	
	public String getProductName(){
		return productName;
	}
	
	public int getProductQuantity(){
		return quantity;
	}
}

/**
 * This is a utility class to provide the thread safe feature for inventory allocation
 * During the inventory allocation, multiple threads might simultaneously access the 
 * product, reduce the quantity of products. needs to have safe way to reduce the quantity
 * of products. it makes sure for each product inventory never below zero and it's thread safe.
 * This data type is used for quantity stored in inventory. 
 * 
 * note: initially I would like to use the AtomicInteger, but found out it can not provide 
 * needs we want in this case.
 * <p>
 */
class SafeInteger {
	private int value;
	
	SafeInteger(int v){
		this.value = v;
	}
	
	public synchronized boolean decreaseBy(int data){
		if(value >= data){
			value -= data;
			return true;
		}
		else
			return false;
    }
	
	public synchronized boolean isZero(){
		return value == 0;
	}
	
	public synchronized String toString(){
		return String.valueOf(value);
	}
}

/**
 * 
 * This class is used to represent the inventory allocation history, 
 * it used for final print out the input order, filled order and back order.
 * it only stored orderLine and filledOrder, the back order information can be
 * derived from the orderLine and filledOrder.
 * <p>
 */
class OrderStatus{
	private final ArrayList<OrderLine> orderLine;
	private final ArrayList<OrderLine> filledOrder;
	
	OrderStatus(ArrayList<OrderLine> orderLine){
		this.orderLine = orderLine;
		this.filledOrder= null;
	}
	
	OrderStatus(ArrayList<OrderLine> orderLine, ArrayList<OrderLine> filledOrder){
		this.orderLine = orderLine;
		this.filledOrder= filledOrder;
	}
	
	public ArrayList<OrderLine> getOrderLine(){
		return orderLine;
	}
	
	public ArrayList<OrderLine> getFilledOrder(){
		return filledOrder;
	}
}
