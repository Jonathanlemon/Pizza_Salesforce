package cpsc4620;

public class PickupOrder extends Order
{

	private int isPickedUp;
	
	public PickupOrder(int orderID, int custID, String date, double custPrice, double busPrice, int isPickedUp, int isComplete) {
		super(orderID, custID, DBNinja.pickup, date, custPrice, busPrice, isComplete);
		this.isPickedUp = isPickedUp;
	}

	public int getIsPickedUp() {
		return isPickedUp;
	}

	public void setIsPickedUp(int isPickedUp) {
		this.isPickedUp = isPickedUp;
	}
	
	@Override
	public String toString() {
		return super.toString() + " | is the order picked up? (yes=1): " + isPickedUp;
	}
}
