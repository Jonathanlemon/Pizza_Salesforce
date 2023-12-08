package cpsc4620;

public class DeliveryOrder extends Order
{
	private String Address;
	
	public DeliveryOrder(int orderID, int custID, String date, double custPrice, double busPrice, int isComplete, String address) 
	{
		super(orderID, custID, DBNinja.delivery, date, custPrice, busPrice, isComplete);
		this.Address = address;
	}

	public String getAddress() {
		return Address;
	}

	public void setAddress(String address) {
		Address = address;
	}

	@Override
	public String toString() {
		return super.toString() + " | Delivered to: " + Address;
	}
	
	
}
