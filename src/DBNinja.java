package cpsc4620;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

/*
 * This file is where most of your code changes will occur You will write the code to retrieve
 * information from the database, or save information to the database
 * 
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 * 
 * This class also has static string variables for pickup, delivery and dine-in. If your database
 * stores the strings differently (i.e "pick-up" vs "pickup") changing these static variables will
 * ensure that the comparison is checking for the right string in other places in the program. You
 * will also need to use these strings if you store this as boolean fields or an integer.
 * 
 * 
 */

/**
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// Change these variables to however you record dine-in, pick-up and delivery, and sizes and crusts
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";


	public static Order make_order_from_rset(ResultSet r) throws SQLException, IOException {
		Order o;

		if(r.getInt("AnOrderTable") > 0){
			//Dine In
			o = new DineinOrder(r.getInt("AnOrderID"), r.getInt("AnOrderCustomerID"), r.getString("AnOrderTime"), r.getDouble("AnOrderPrice"), r.getDouble("AnOrderCost"), r.getInt("AnOrderState"), r.getInt("AnOrderTable"));
		}
		else if(r.getInt("AnOrderPickedUp") == 1 || r.getInt("AnOrderPickedUp") == 0){
			//Pick up
			o = new PickupOrder(r.getInt("AnOrderID"), r.getInt("AnOrderCustomerID"), r.getString("AnOrderTime"), r.getDouble("AnOrderPrice"), r.getDouble("AnOrderCost"), r.getInt("AnOrderPickedUp"), r.getInt("AnOrderState"));
		}
		else{
			//Delivery
			//Need to fetch address information from customer via customer ID
			String address = getAddress(r.getInt("AnOrderCustomerID"));
			o = new DeliveryOrder(r.getInt("AnOrderID"), r.getInt("AnOrderCustomerID"), r.getString("AnOrderTime"), r.getDouble("AnOrderPrice"), r.getDouble("AnOrderCost"), r.getInt("AnOrderState"), address);
		}

		return o;
	}

	public static String getAddress(int custID) throws SQLException, IOException {
		connect_to_db();
		//Update pizza price
		String query = "select CONCAT(CustomerAddress,\" \",CustomerCity,\" \",CustomerState,\" \",CustomerZip) as \"address\" from customer where CustomerID = ?;";
		PreparedStatement os = conn.prepareStatement(query);
		os.setInt(1, custID);
		ResultSet rset = os.executeQuery();
		rset.next();
		conn.close();
		return rset.getString("address");
	}

	public static void setAddress(int custID, String address) throws SQLException, IOException {
		connect_to_db();
		String streetaddress = "";
		String city = "";
		String state = "";
		String zip = "";

		String[] splitStr = address.split("\\s+");
		zip = splitStr[splitStr.length-1];
		state = splitStr[splitStr.length-2];
		city = splitStr[splitStr.length-3];
		streetaddress += splitStr[0];
		for(int i=1; i < splitStr.length-3; i++){
			streetaddress += " ";
			streetaddress += splitStr[i];
		}
		//Update pizza price
		String query = "Update customer set CustomerAddress = ?, CustomerCity = ?, CustomerState = ?, CustomerZip = ? where CustomerID = ?";
		PreparedStatement os = conn.prepareStatement(query);
		os.setString(1, streetaddress);
		os.setString(2, city);
		os.setString(3, state);
		os.setInt(4, Integer.parseInt(zip));
		os.setInt(5, custID);
		os.executeUpdate();
		conn.close();
		return;
	}

	
	private static boolean connect_to_db() throws SQLException, IOException {

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	
	public static void addOrder(Order o) throws SQLException, IOException 
	{
		connect_to_db();
		//Update pizza price
		String query = "Insert into anOrder values (?, ?, 0, 0.0, 0.0, NOW(), ?, ?, ?)";
		PreparedStatement os = conn.prepareStatement(query);
		os.setInt(1, o.getOrderID());
		os.setString(2, o.getOrderType());
		if(o instanceof DineinOrder){
			os.setInt(3, ((DineinOrder)o).getTableNum());
			os.setNull(4, java.sql.Types.NULL);
			os.setInt(5, 1);
		}
		else if(o instanceof PickupOrder){
			os.setNull(3, java.sql.Types.NULL);
			os.setInt(4, 0);
			os.setInt(5, o.getCustID());
		}
		else if(o instanceof DeliveryOrder){
			os.setNull(3, java.sql.Types.NULL);
			os.setNull(4, java.sql.Types.NULL);
			os.setInt(5, o.getCustID());
			setAddress(o.getCustID(), ((DeliveryOrder)o).getAddress());
		}
		else{
			System.out.println("Invalid order type!");
			conn.close();
			return;
		}
		os.executeUpdate();
		conn.close();
	}
	
	public static void addPizza(Pizza p) throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		String query;

		//Update inventory
		query = "Insert into pizza values (?, ?, ?, ?, 0, ?, ?)";
		os = conn.prepareStatement(query);
		os.setInt(1, p.getPizzaID());
		os.setInt(2, p.getOrderID());
		os.setString(3, p.getCrustType());
		os.setString(4, p.getSize());
		os.setDouble(5, p.getCustPrice());
		os.setDouble(6, p.getBusPrice());
		os.executeUpdate();
		conn.close();
	}
	
	
	public static void useTopping(Pizza p, Topping t, boolean isDoubled) throws SQLException, IOException //this method will update toppings inventory in SQL and add entities to the Pizzatops table. Pass in the p pizza that is using t topping
	{
		int multiplier = 1;
		if(isDoubled){
			multiplier = 2;
		}
		connect_to_db();
		//Update pizza price
		String query = "Update pizza set PizzaPrice = PizzaPrice + ?, PizzaCost = PizzaCost + ? where PizzaID = ?";
		PreparedStatement os;
		os = conn.prepareStatement(query);
		os.setDouble(1, t.getCustPrice()*multiplier);
		os.setDouble(2, t.getBusPrice()*multiplier);
		os.setInt(3, p.getPizzaID());
		os.executeUpdate();
		conn.close();

		double toppingAmt = 0.0;
		switch (p.getSize()) {
			case size_s:
				toppingAmt = t.getPerAMT();
				break;
			case size_m:
				toppingAmt = t.getMedAMT();
				break;
			case size_l:
				toppingAmt = t.getLgAMT();
				break;
			case size_xl:
				toppingAmt = t.getXLAMT();
				System.out.println("Topping xl amount: "+toppingAmt);
				break;
			default:
				break;
		}
		if(isDoubled){
			toppingAmt = toppingAmt*2;
		}

		connect_to_db();
		//Update inventory
		query = "Update topping set ToppingCurInv = ToppingCurInv - ? where ToppingID = ?";
		os = conn.prepareStatement(query);
		os.setInt(1, (int)Math.round(toppingAmt));
		os.setInt(2, t.getTopID());
		os.executeUpdate();
		conn.close();

		connect_to_db();
		//insert into pizza_to_topping
		query = "insert into pizza_to_topping values (?, ?, ?)";
		os = conn.prepareStatement(query);
		os.setInt(1, p.getPizzaID());
		os.setInt(2, t.getTopID());
		int serv = 1;
		if(isDoubled){
			serv = 2;
		}
		os.setInt(3, serv);
		os.executeUpdate();
		conn.close();
	}
	
	
	public static void usePizzaDiscount(Pizza p, Discount d) throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		String query;
		//Insert into order_to_discount
		query = "Insert into pizza_to_discount values (?, ?);";
		os = conn.prepareStatement(query);
		os.setInt(1, p.getPizzaID());
		os.setInt(2, d.getDiscountID());
		os.executeUpdate();
		conn.close();


		connect_to_db();
		//Update order price
		query = "Select * from discount where DiscountID=?";
		os = conn.prepareStatement(query);
		os.setInt(1, d.getDiscountID());
		ResultSet rset = os.executeQuery();
		rset.next();
		int dollarOff = rset.getInt("DiscountDollarOff");
		int percentOff = rset.getInt("DiscountPercentOff");
		conn.close();
		connect_to_db();

		if(dollarOff > 0){
			//Use dollar off
			query = "Update pizza set PizzaPrice = PizzaPrice - ? where PizzaID = ?";
			os = conn.prepareStatement(query);
			os.setInt(1, dollarOff);
			os.setInt(2, p.getPizzaID());
		}
		else{
			query = "Update pizza set PizzaPrice = PizzaPrice * (1-?) where PizzaID = ?";
			os = conn.prepareStatement(query);
			os.setInt(1, percentOff);
			os.setInt(2, p.getPizzaID());
		}
		os.executeUpdate();
		conn.close();
		return;
	}
	
	public static void useOrderDiscount(Order o, Discount d) throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		String query;
		//Insert into order_to_discount
		query = "Insert into order_to_discount values (?, ?);";
		os = conn.prepareStatement(query);
		os.setInt(1, o.getOrderID());
		os.setInt(2, d.getDiscountID());
		os.executeUpdate();
		conn.close();


		connect_to_db();
		//Update order price
		query = "Select * from discount where DiscountID=?";
		os = conn.prepareStatement(query);
		os.setInt(1, d.getDiscountID());
		ResultSet rset = os.executeQuery();
		rset.next();
		int dollarOff = rset.getInt("DiscountDollarOff");
		int percentOff = rset.getInt("DiscountPercentOff");
		conn.close();
		connect_to_db();

		if(dollarOff > 0){
			//Use dollar off
			query = "Update anOrder set AnOrderPrice = AnOrderPrice - ? where AnOrderID = ?";
			os = conn.prepareStatement(query);
			os.setInt(1, dollarOff);
			os.setInt(2, o.getOrderID());
		}
		else{
			query = "Update anOrder set AnOrderPrice = AnOrderPrice * (1-?) where AnOrderID = ?";
			os = conn.prepareStatement(query);
			os.setInt(1, percentOff);
			os.setInt(2, o.getOrderID());
		}
		os.executeUpdate();
		conn.close();
		return;
	}
	
	public static void addCustomer(Customer c) throws SQLException, IOException {
		connect_to_db();
		int areaCode = Integer.parseInt(c.getPhone().substring(0, 3));
		int phoneNum = Integer.parseInt(c.getPhone().substring(3));
		PreparedStatement os;
		String query;
		query = "Insert into customer values (null, ?, ?, ?, ?, null, null, null, null);";
		os = conn.prepareStatement(query);
		os.setString(1, c.getFName());
		os.setString(2, c.getLName());
		os.setInt(3, areaCode);
		os.setInt(4, phoneNum);
		os.executeUpdate();
		conn.close();
		return;
	}

	public static void completeOrder(Order o) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		String query;
		query = "Update anOrder set anOrderState = 1 where AnOrderID = ?;";
		os = conn.prepareStatement(query);
		os.setInt(1, o.getOrderID());
		os.executeUpdate();
		conn.close();
		connect_to_db();
		query = "Update pizza set PizzaState = 1 where PizzaAnOrderID = ?;";
		os = conn.prepareStatement(query);
		os.setInt(1, o.getOrderID());
		os.executeUpdate();
		conn.close();
		return;

	}


	public static ArrayList<Order> getOrders(boolean openOnly) throws SQLException, IOException {
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from anOrder;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		ArrayList<Order> list = new ArrayList<Order>();
		while(rset.next())
		{
			int myInt = rset.getBoolean("AnOrderState") ? 1 : 0;
			if(openOnly){
				if(myInt == 0){
					list.add(make_order_from_rset(rset));
				}
			}
			else{
				list.add(make_order_from_rset(rset));
			}
		}

		conn.close();
		return list;
	}
	
	public static Order getLastOrder() throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from anOrder where AnOrderTime = (Select MAX(AnOrderTime) from anOrder);";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		Order o = new Order(-1, -1, "Dummy", "Dummy", 0.0, 0.0, 1);
		while(rset.next())
		{
			o = make_order_from_rset(rset);
		}

		conn.close();
		return o;
	}

	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException{
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from anOrder where DATE(AnOrderTime) >= ?;";
		os = conn.prepareStatement(query);
		os.setString(1, date);
		rset = os.executeQuery();
		ArrayList<Order> orders = new ArrayList<Order>();
		while(rset.next())
		{
			orders.add(make_order_from_rset(rset));
		}

		conn.close();
		return orders;
	}
		
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from discount;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		ArrayList<Discount> discounts = new ArrayList<Discount>();
		while(rset.next())
		{
			Discount d = new Discount(rset.getInt("DiscountID"), rset.getString("DiscountName"), 0, false);
			if(rset.getDouble("DiscountPercentOff") > 0){
				d.setAmount(rset.getDouble("DiscountPercentOff"));
				d.setPercent(true);
			}
			else{
				d.setAmount(rset.getDouble("DiscountDollarOff"));
			}
			discounts.add(d);
		}

		conn.close();
		return discounts;
	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException{
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from discount where DiscountName = ?;";
		os = conn.prepareStatement(query);
		os.setString(1, name);
		rset = os.executeQuery();
		Discount d = null;
		while(rset.next())
		{
			d = new Discount(rset.getInt("DiscountID"), rset.getString("DiscountName"), 0, false);
			if(rset.getDouble("DiscountPercentOff") > 0){
				d.setAmount(rset.getDouble("DiscountPercentOff"));
				d.setPercent(true);
			}
			else{
				d.setAmount(rset.getDouble("DiscountDollarOff"));
			}
		}

		conn.close();
		return d;
	}


	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException {
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from customer order by CustomerLastName, CustomerFirstName, CustomerAreaCode, CustomerPhone asc;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		ArrayList<Customer> list = new ArrayList<Customer>();
		while(rset.next())
		{
			list.add(new Customer(Integer.parseInt(rset.getString("CustomerID")), rset.getString("CustomerFirstName"), rset.getString("CustomerLastName"), rset.getString("CustomerAreaCode") + rset.getString("CustomerPhone")));
		}

		conn.close();
		return list;
	}

	public static Customer findCustomerByPhone(String phoneNumber) throws SQLException, IOException{
	
		try {
			int d = Integer.parseInt(phoneNumber);
		} catch (NumberFormatException nfe) {
			return null;
		}
		
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from customer where CustomerAreaCode = ? and CustomerPhone = ?;";
		os = conn.prepareStatement(query);
		os.setInt(1, Integer.parseInt(phoneNumber.substring(0,3)));
		os.setInt(2, Integer.parseInt(phoneNumber.substring(3)));
		rset = os.executeQuery();
		Customer c = null;
		while(rset.next())
		{
			c = new Customer(Integer.parseInt(rset.getString("CustomerID")), rset.getString("CustomerFirstName"), rset.getString("CustomerLastName"), rset.getString("CustomerAreaCode") + rset.getString("CustomerPhone"));
		}

		conn.close();
		return c;
	}


	public static ArrayList<Topping> getToppingList() throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from topping";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		ArrayList<Topping> list = new ArrayList<Topping>();
		while(rset.next())
		{
			list.add(new Topping(rset.getInt("ToppingID"), rset.getString("ToppingName"), 0.0, 0.0, 0.0, 0.0, rset.getDouble("ToppingPrice"), rset.getDouble("ToppingCost"), rset.getInt("ToppingMinInv"), rset.getInt("ToppingCurInv")));
		}

		conn.close();
		updateAmounts(list);
		return list;
	}

	private static void updateAmounts(ArrayList<Topping> list) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from topping_to_serving";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		int i = 0;
		int count = 0;
		while(rset.next())
		{
			if(rset.getString("Size").equals("Large")){
				list.get(i).setLgAMT(rset.getDouble("Serving"));
				count++;
			}
			if(rset.getString("Size").equals("Medium")){
				list.get(i).setMedAMT(rset.getDouble("Serving"));
				count++;
			}
			if(rset.getString("Size").equals("Small")){
				list.get(i).setPerAMT(rset.getDouble("Serving"));
				count++;
			}
			if(rset.getString("Size").equals("XLarge")){
				list.get(i).setXLAMT(rset.getDouble("Serving"));
				count++;
			}
			if(count == 4){
				i++;
				count = 0;
			}
		}

		conn.close();
		return;
	}

	private static void updateAmounts(Topping t) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from topping_to_serving where ToppingID = ?";
		os = conn.prepareStatement(query);
		os.setInt(1, t.getTopID());
		rset = os.executeQuery();
		while(rset.next())
		{
			if(rset.getString("Size").equals("Large")){
				t.setLgAMT(rset.getDouble("Serving"));
			}
			if(rset.getString("Size").equals("Medium")){
				t.setMedAMT(rset.getDouble("Serving"));
			}
			if(rset.getString("Size").equals("Small")){
				t.setPerAMT(rset.getDouble("Serving"));
			}
			if(rset.getString("Size").equals("XLarge")){
				t.setXLAMT(rset.getDouble("Serving"));
			}
		}

		conn.close();
		return;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException{
		Topping t = null;
		
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from topping where ToppingName = ?";
		os = conn.prepareStatement(query);
		os.setString(1, name);
		rset = os.executeQuery();
		while(rset.next())
		{
			t = new Topping(rset.getInt("ToppingID"), name, 0.0, 0.0, 0.0, 0.0, rset.getDouble("ToppingPrice"), rset.getDouble("ToppingCost"), rset.getInt("ToppingMinInv"), rset.getInt("ToppingCurInv"));
		}

		updateAmounts(t);

		conn.close();
		return t;
	}


	public static void addToInventory(Topping t, double quantity) throws SQLException, IOException {
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Update topping set ToppingCurInv = ToppingCurInv + ? where ToppingID = ?;";
		os = conn.prepareStatement(query);
		os.setDouble(1, quantity);
		os.setInt(2, t.getTopID());
		os.executeUpdate();
		conn.close();
		return;
	}
	
	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException {
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select BasePrice from base where BaseCrust = ? and BaseSize = ?;";
		os = conn.prepareStatement(query);
		os.setString(1, crust);
		os.setString(2, size);
		rset = os.executeQuery();
		double price = 0.0;
		while(rset.next())
		{
			price = rset.getDouble("BasePrice");
		}
		conn.close();
		//DO NOT FORGET TO CLOSE YOUR CONNECTION
		return price;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException {
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select BaseCost from base where BaseCrust = ? and BaseSize = ?;";
		os = conn.prepareStatement(query);
		os.setString(1, crust);
		os.setString(2, size);
		rset = os.executeQuery();
		double cost = 0.0;
		while(rset.next())
		{
			cost = rset.getDouble("BaseCost");
		}
		conn.close();
		return cost;
	}

	public static void printInventory() throws SQLException, IOException {
		connect_to_db();

		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from topping;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		System.out.println("ID:  Name         CurINVT");
		while(rset.next())
		{
			System.out.println(rset.getInt("ToppingID")+"  "+rset.getString("ToppingName")+"         "+rset.getInt("ToppingCurInv"));
		}
		conn.close();
		return;
	}
	
	public static void printToppingPopReport() throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from ToppingPopularity;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		while(rset.next())
		{
			System.out.println(rset.getString("Topping") + " " + rset.getString("ToppingCount"));
		}

		conn.close();
	}
	
	public static void printProfitByPizzaReport() throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from ProfitByPizza;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		while(rset.next())
		{
			System.out.println(rset.getString("Size") + " " + rset.getString("Crust") + " " + rset.getString("Profit") + " " + rset.getString("Order Month"));
		}

		conn.close();
	}
	
	public static void printProfitByOrderType() throws SQLException, IOException
	{
		connect_to_db();
		PreparedStatement os;
		ResultSet rset;
		String query;
		query = "Select * from ProfitByOrderType;";
		os = conn.prepareStatement(query);
		rset = os.executeQuery();
		while(rset.next())
		{
			System.out.println(rset.getString("customerType") + " " + rset.getString("Order Month") + " " + rset.getString("TotalOrderPrice") + " " + rset.getString("TotalOrderCost") + " " + rset.getString("Profit"));
		}

		conn.close();
	}
	
	
	
	public static String getCustomerName(int CustID) throws SQLException, IOException
	{

		 connect_to_db();

		/* 
		 * an example query using a constructed string...
		 * remember, this style of query construction could be subject to sql injection attacks!
		 * 
		 */
		String cname1 = "";
		String query = "Select CustomerFirstName, CustomerLastName From customer WHERE CustomerID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);
		
		while(rset.next())
		{
			cname1 = rset.getString(1) + " " + rset.getString(2); 
		}

		/* 
		* an example of the same query using a prepared statement...
		* 
		*/
		String cname2 = "";
		PreparedStatement os;
		ResultSet rset2;
		String query2;
		query2 = "Select CustomerFirstName, CustomerLastName From customer WHERE CustomerID=?;";
		os = conn.prepareStatement(query2);
		os.setInt(1, CustID);
		rset2 = os.executeQuery();
		while(rset2.next())
		{
			cname2 = rset2.getString("CustomerFirstName") + " " + rset2.getString("CustomerLastName"); // note the use of field names in the getSting methods
		}

		conn.close();
		return cname1; // OR cname2
	}


	public static int getNewOrderID() throws SQLException, IOException
		{
			connect_to_db();
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select MAX(AnOrderID) from anOrder;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			rset.next();
			int num =  rset.getInt("Max(AnOrderID)") + 1;
			conn.close();
			return num;
		}

	public static int getNewPizzaID() throws SQLException, IOException
		{
			connect_to_db();
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select MAX(PizzaID) from pizza;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			rset.next();
			int num = rset.getInt("Max(PizzaID)") + 1;
			conn.close();
			return num;
		}

		public static int getNewCustID() throws SQLException, IOException
		{
			connect_to_db();
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select MAX(CustomerID) from customer;";
			os = conn.prepareStatement(query);
			rset = os.executeQuery();
			rset.next();
			int num = rset.getInt("Max(CustomerID)") + 1;
			conn.close();
			return num;
		}

		public static void calculateOrderPrice(Order o) throws SQLException, IOException
		{
			connect_to_db();
			PreparedStatement os;
			ResultSet rset;
			String query;
			query = "Select SUM(PizzaPrice), SUM(PizzaCost) from pizza where PizzaAnOrderID = ?;";
			os = conn.prepareStatement(query);
			os.setInt(1, o.getOrderID());
			rset = os.executeQuery();
			rset.next();
			double price = rset.getDouble("SUM(PizzaPrice)");
			double cost = rset.getDouble("SUM(PizzaCost)");
			conn.close();
			connect_to_db();
			query = "Update anOrder set anOrderPrice = ?, anOrderCost = ? where anOrderID = ?";
			os = conn.prepareStatement(query);
			os.setDouble(1, price);
			os.setDouble(2, cost);
			os.setInt(3, o.getOrderID());
			os.executeUpdate();
			conn.close();
			return;
		}

	/*
	 * The next 3 private methods help get the individual components of a SQL datetime object. 
	 * You're welcome to keep them or remove them.
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0,4));
	}
	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}
	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder)
	{
		if(getYear(dateOfOrder) > year)
			return true;
		else if(getYear(dateOfOrder) < year)
			return false;
		else
		{
			if(getMonth(dateOfOrder) > month)
				return true;
			else if(getMonth(dateOfOrder) < month)
				return false;
			else
			{
				if(getDay(dateOfOrder) >= day)
					return true;
				else
					return false;
			}
		}
	}


}