package cpsc4620;

public class Customer 
{
	private int CustID;
	private String FName;
	private String LName;
	private String Phone;
	private String Address;

	public Customer(int custID, String fName, String lName, String phone) {
		CustID = custID;
		FName = fName;
		LName = lName;
		Phone = phone;
	}

	public int getCustID() {
		return CustID;
	}

	public String getFName() {
		return FName;
	}

	public String getLName() {
		return LName;
	}

	public String getPhone() {
		return Phone;
	}

	public String getAddress() { return Address; }

	public void setCustID(int custID) {
		CustID = custID;
	}

	public void setFName(String fName) {
		FName = fName;
	}

	public void setLName(String lName) {
		LName = lName;
	}

	public void setPhone(String phone) {
		Phone = phone;
	}

	public void setAddress (String street, String city, String state, String zip) {
		Address = street + "/n" + city + "/n" + state + "/n" + zip;
	}

	@Override
	public String toString() {
		return "CustID=" + CustID + " | Name= " + FName +  " " + LName + ", Phone= " + Phone;
	}
	
	
}
