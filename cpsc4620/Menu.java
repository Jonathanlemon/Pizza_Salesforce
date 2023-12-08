package cpsc4620;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mysql.cj.x.protobuf.MysqlxDatatypes.Array;

/*
 * This file is where the front end magic happens.
 * 
 * You will have to write the methods for each of the menu options.
 * 
 * This file should not need to access your DB at all, it should make calls to the DBNinja that will do all the 0ions.
 * 
 * You can add and remove methods as you see necessary. But you MUST have all of the menu methods (including exit!)
 * 
 * Simply removing menu methods because you don't know how to implement it will result in a major error penalty (akin to your program crashing)
 * 
 * Speaking of crashing. Your program shouldn't do it. Use exceptions, or if statements, or whatever it is you need to do to keep your program from breaking.
 * 
 */

public class Menu {

	public static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	public static void main(String[] args) throws SQLException, IOException {

		System.out.println("Welcome to Pizzas-R-Us!");
		int menu_option = 0;
		// present a menu of options and take their selection
		
		PrintMenu();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String option = reader.readLine();
		menu_option = Integer.parseInt(option);

		while (menu_option != 9) {
			switch (menu_option) {
			case 1:// enter order
				EnterOrder();
				break;
			case 2:// view customers
				viewCustomers();
				break;
			case 3:// enter customer
				EnterCustomer();
				break;
			case 4:// view order
				// open/closed/date
				ViewOrders();
				break;
			case 5:// mark order as complete
				MarkOrderAsComplete();
				break;
			case 6:// view inventory levels
				ViewInventoryLevels();
				break;
			case 7:// add to inventory
				AddInventory();
				break;
			case 8:// view reports
				PrintReports();
				break;
			}
			PrintMenu();
			option = reader.readLine();
			menu_option = Integer.parseInt(option);
		}

	}

	// allow for a new order to be placed
	public static void EnterOrder() throws SQLException, IOException 
	{
		String line;
		int option;


		Order o;
		String orderType;
		int custID = 1;
		int tableNum = -1;
		String address = "";
		int orderNumber = DBNinja.getNewOrderID();
		double price = 0.0;
		double cost = 0.0;
		/*
		 * EnterOrder should do the following:
		 * 
		 * Ask if the order is delivery, pickup, or dinein
		 *   if dine in....ask for table number
		 *   if pickup...
		 *   if delivery...
		 * 
		 * Then, build the pizza(s) for the order (there's a method for this)
		 *  until there are no more pizzas for the order
		 *  add the pizzas to the order
		 *
		 * Apply order discounts as needed (including to the DB)
		 * 
		 * return to menu
		 * 
		 * make sure you use the prompts below in the correct order!
		 */

		 // User Input Prompts...
		System.out.println("Is this order for: \n1.) Dine-in\n2.) Pick-up\n3.) Delivery\nEnter the number of your choice:");
		line = reader.readLine();
		option = Integer.parseInt(line);
		if(option == 1){
			orderType = DBNinja.dine_in;
			System.out.println("What is the table number for this order?");
			line = reader.readLine();
			tableNum = Integer.parseInt(line);
		}
		else if(option == 2 || option == 3){
			orderType = (option == 2) ? DBNinja.pickup : DBNinja.delivery;
			boolean done;
			do{
				done = true;
				System.out.println("Is this order for an existing customer? Answer y/n: ");
				line = reader.readLine();
				String c = line.substring(0,1);
				if(c.equals("y")){
					System.out.println("Here's a list of the current customers: ");
					//Print customers
					viewCustomers();
					System.out.println("Which customer is this order for? Enter ID Number:");
					custID = Integer.parseInt(reader.readLine());
				}
				else if (c.equals("n")){
					EnterCustomer();
					custID = DBNinja.getNewCustID() - 1;
				}
				else{
					done = false;
					System.out.println("ERROR: I don't understand your input for: Is this order an existing customer?");
				}
			}while(!done);

			if(orderType == DBNinja.delivery){
				System.out.println("What is the House/Apt Number for this order? (e.g., 111)");
				address += reader.readLine();
				address += " ";
				System.out.println("What is the Street for this order? (e.g., Smile Street)");
				address += reader.readLine();
				address += " ";
				System.out.println("What is the City for this order? (e.g., Greenville)");
				address += reader.readLine();
				address += " ";
				System.out.println("What is the State for this order? (e.g., SC)");
				address += reader.readLine();
				address += " ";
				System.out.println("What is the Zip Code for this order? (e.g., 20605)");
				address += reader.readLine();
			}
		}
		else{
			System.out.println("Something went wrong!");
			return;
		}

		if(orderType == DBNinja.pickup){
			o = new PickupOrder(orderNumber, custID, "NOW()", price, cost, 0, 0);
		}
		else if(orderType == DBNinja.delivery){
			o = new DeliveryOrder(orderNumber, custID, "NOW()", price, cost, 0, address);
		}
		else{
			o = new DineinOrder(orderNumber, custID, "NOW()", price, cost, 0, tableNum);
		}

		DBNinja.addOrder(o);

		//Pizzas
		int pizzaLoop = 0;
		do{
			System.out.println("Let's build a pizza!");
			Pizza p = buildPizza(orderNumber);
			o.addPizza(p);
			price += p.getCustPrice();
			cost += p.getBusPrice();
			System.out.println("Enter -1 to stop adding pizzas...Enter anything else to continue adding pizzas to the order.");
			pizzaLoop = Integer.parseInt(reader.readLine());
		}while(pizzaLoop != -1);
		//After the last pizza is added, recalculate the order price and cost
		DBNinja.calculateOrderPrice(o);

		//Discounts
		System.out.println("Do you want to add discounts to this order? Enter y/n?");
		line = reader.readLine();
		if(line.substring(0, 1).equals("y")){
			//Discounts are are applied after the order is inserted into the database. The DBNinja function will handle updating the price
			ArrayList<Discount> d = DBNinja.getDiscountList();
			ArrayList<Integer> discountIDs = new ArrayList<Integer>();
			for (Discount d2 : d) {
				System.out.println(d2);
				discountIDs.add(d2.getDiscountID());
			}
			int choice = 0;
			do{
				System.out.println("Which Order Discount do you want to add? Enter the DiscountID. Enter -1 to stop adding Discounts: ");
				choice = Integer.parseInt(reader.readLine());
				if(choice == -1){
					break;
				}
				DBNinja.useOrderDiscount(o, new Discount(choice, "Dummy", 0.0, false));
			}while(choice != -1);
		}

		System.out.println("Finished adding order...Returning to menu...");
	}
	
	
	public static void viewCustomers() throws SQLException, IOException 
	{
		ArrayList<Customer> customers = DBNinja.getCustomerList();
		customers.forEach((n) -> System.out.println(n));
	}
	

	// Enter a new customer in the database
	public static void EnterCustomer() throws SQLException, IOException 
	{
		System.out.println("What is this customer's name (first <space> last");
		String name = reader.readLine();
		System.out.println("What is this customer's phone number (##########) (No dash/space)");
		String phone = reader.readLine();
		DBNinja.addCustomer(new Customer(-1, name.split(" ")[0], name.split(" ")[1], phone));
	}

	// View any orders that are not marked as completed
	public static void ViewOrders() throws SQLException, IOException 
	{
		/*  
		* This method allows the user to select between three different views of the Order history:
		* The program must display:
		* a.	all open orders
		* b.	all completed orders 
		* c.	all the orders (open and completed) since a specific date (inclusive)
		* 
		* After displaying the list of orders (in a condensed format) must allow the user to select a specific order for viewing its details.  
		* The details include the full order type information, the pizza information (including pizza discounts), and the order discounts.
		* 
		*/ 
		// User Input Prompts...

		ArrayList<Order> orders = new ArrayList<Order>();
		ArrayList<Integer> orderNumbers = new ArrayList<Integer>();
		System.out.println("Would you like to:\n(a) display all orders [open or closed]\n(b) display all open orders\n(c) display all completed [closed] orders\n(d) display orders since a specific date");
		String line = reader.readLine();
		if(!(line.equals("a") || line.equals("b") || line.equals("c") || line.equals("d"))){
			System.out.println("I don't understand that input, returning to menu");
			return;
		}
		else if(line.equals("d")){
			System.out.println("What is the date you want to restrict by? (FORMAT= YYYY-MM-DD)");
			String date = reader.readLine();
			orders = DBNinja.getOrdersByDate(date);
			for (Order o : orders) {
				System.out.println(o.toSimplePrint());
				orderNumbers.add(o.getOrderID());
			}
		}
		else if(line.equals("a")){
			orders = DBNinja.getOrders(false);
			for (Order o : orders) {
				System.out.println(o.toSimplePrint());
				orderNumbers.add(o.getOrderID());
			}
		}
		else if(line.equals("b")){
			orders = DBNinja.getOrders(true);
			for (Order o : orders) {
				System.out.println(o.toSimplePrint());
				orderNumbers.add(o.getOrderID());
			}	
		}
		else{
			orders = DBNinja.getOrders(false);
			for (Order o : orders) {
				if(o.getIsComplete() == 1){
					System.out.println(o.toSimplePrint());
					orderNumbers.add(o.getOrderID());
				}
			}
		}

				
		if(orders.size() == 0){
			System.out.println("No orders to display, returning to menu.");
			return;
		}

		//Get choice for details
		int selection = 0;
		System.out.println("Which order would you like to see in detail? Enter the number (-1 to exit): ");
		selection = Integer.parseInt(reader.readLine());
		if(selection == -1){
			return;
		}
		if(!orderNumbers.contains(selection)){
			System.out.println("Incorrect entry, returning to menu.");
			return;
		}


		System.out.println(orders.get(orderNumbers.indexOf(selection)));

		return;
	}

	
	// When an order is completed, we need to make sure it is marked as complete
	public static void MarkOrderAsComplete() throws SQLException, IOException 
	{
		ArrayList<Order> orders = DBNinja.getOrders(true);
		if(orders.size() == 0){
			System.out.println("There are no open orders currently... returning to menu...");
			return;
		}
		else{
			int selectedOrder=0;
			boolean valid = false;
			ArrayList<Integer> orderIDs = new ArrayList<Integer>();
			for(Order o : orders) {
				System.out.println(o.toSimplePrint());
				orderIDs.add(o.getOrderID());
			}
			do{
				System.out.println("Which order would you like mark as complete? Enter the OrderID: ");
				selectedOrder = Integer.parseInt(reader.readLine());
				if(!orderIDs.contains(selectedOrder)){
					System.out.println("Incorrect entry, not an option");
				}
				else{
					valid = true;
				}
			}while(!valid);
			DBNinja.completeOrder(new Order(selectedOrder, 0, "Dummy", "Dummy", 0, 0, 0));
		}
	}

	public static void ViewInventoryLevels() throws SQLException, IOException 
	{
		DBNinja.printInventory();
	}


	public static void AddInventory() throws SQLException, IOException 
	{
		DBNinja.printInventory();
		// User Input Prompts...
		int choice = 0;
		ArrayList<Integer> toppingIDs = new ArrayList<Integer>();
		ArrayList<Topping> toppings = DBNinja.getToppingList();
		for (Topping t : toppings) {
			toppingIDs.add(t.getTopID());
		}
		do{
			System.out.println("Which topping do you want to add inventory to? Enter the number: ");
			choice = Integer.parseInt(reader.readLine());
			if(!toppingIDs.contains(choice)){
				System.out.println("Incorrect entry, not an option");
			}
		}while(!toppingIDs.contains(choice));

		System.out.println("How many units would you like to add? ");
		double amt = Double.parseDouble(reader.readLine());
		DBNinja.addToInventory(new Topping(choice, "Dummy", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0), amt);
	}

	// A method that builds a pizza. Used in our add new order method
	public static Pizza buildPizza(int orderID) throws SQLException, IOException 
	{
		
		/*
		 * This is a helper method for first menu option.
		 * 
		 * It should ask which size pizza the user wants and the crustType.
		 * 
		 * Once the pizza is created, it should be added to the DB.
		 * 
		 * We also need to add toppings to the pizza. (Which means we not only need to add toppings here, but also our bridge table)
		 * 
		 * We then need to add pizza discounts (again, to here and to the database)
		 * 
		 * Once the discounts are added, we can return the pizza
		 */

		Pizza ret = null;
		String size = "";
		String crust = "";
		double price = 0.0;
		double cost = 0.0;
		int pizzaID = DBNinja.getNewPizzaID();

		// User Input Prompts...
		System.out.println("What size is the pizza?");
		System.out.println("1."+DBNinja.size_s);
		System.out.println("2."+DBNinja.size_m);
		System.out.println("3."+DBNinja.size_l);
		System.out.println("4."+DBNinja.size_xl);
		System.out.println("Enter the corresponding number: ");
		int sizeNum = Integer.parseInt(reader.readLine());
		switch (sizeNum) {
			case 1:
				size = DBNinja.size_s;
				break;
			case 2:
				size = DBNinja.size_m;
				break;
			case 3:
				size = DBNinja.size_l;
				break;
			case 4:
				size = DBNinja.size_xl;
				break;
			default:
				break;
		}
		System.out.println("What crust for this pizza?");
		System.out.println("1."+DBNinja.crust_thin);
		System.out.println("2."+DBNinja.crust_orig);
		System.out.println("3."+DBNinja.crust_pan);
		System.out.println("4."+DBNinja.crust_gf);
		System.out.println("Enter the corresponding number: ");
		int crustNum = Integer.parseInt(reader.readLine());
		switch (crustNum) {
			case 1:
				crust = DBNinja.crust_thin;
				break;
			case 2:
				crust = DBNinja.crust_orig;
				break;
			case 3:
				crust = DBNinja.crust_pan;
				break;
			case 4:
				crust = DBNinja.crust_gf;
				break;
			default:
				break;
		}


		//Build pizza base
		price += DBNinja.getBaseCustPrice(size, crust);
		cost += DBNinja.getBaseBusPrice(size, crust);

		ret = new Pizza(pizzaID, size, crust, orderID, "not_done", "NOW()", price, cost);
		DBNinja.addPizza(ret);

		//Handle toppings
		System.out.println("ID:  Name         CurINV");
		ArrayList<Topping> availableToppings = DBNinja.getToppingList();
		availableToppings.forEach((n) -> System.out.println(n.getTopID()+"  "+n.getTopName()+"          "+n.getCurINVT()));
		int topping = 0;
		double toppingAmt = 0.0;
		do{
			System.out.println("Which topping do you want to add? Enter the TopID. Enter -1 to stop adding toppings: ");
			topping = Integer.parseInt(reader.readLine());
			if(topping == -1){
				break;
			}
			System.out.println("Do you want to add extra topping? Enter y/n");
			String extra = reader.readLine();
			
			switch (size) {
				case DBNinja.size_s:
					toppingAmt =availableToppings.get(topping-1).getPerAMT();
					break;
				case DBNinja.size_m:
					toppingAmt =availableToppings.get(topping-1).getMedAMT();
					break;
				case DBNinja.size_l:
					toppingAmt =availableToppings.get(topping-1).getLgAMT();
					break;
				case DBNinja.size_xl:
					toppingAmt =availableToppings.get(topping-1).getLgAMT();
					break;
			
				default:
					break;
			}


			if(extra.equals("y")){
				toppingAmt = toppingAmt * 2;
			}
			if(availableToppings.get(topping-1).getCurINVT() - toppingAmt < availableToppings.get(topping-1).getMinINVT()){
				System.out.println("We don't have enough of that topping to add it...");
			}
			else{
				DBNinja.useTopping(ret, availableToppings.get(topping-1), extra.equals("y"));
			}
		}while(topping != -1);

		System.out.println("Do you want to add discounts to this Pizza? Enter y/n?");

		if(reader.readLine().equals("y")){		
			ArrayList<Discount> d = DBNinja.getDiscountList();
			ArrayList<Integer> discountIDs = new ArrayList<Integer>();
			for (Discount d2 : d) {
				System.out.println(d2);
				discountIDs.add(d2.getDiscountID());
			}
			int choice = 0;
			do{
				System.out.println("Which Pizza Discount do you want to add? Enter the DiscountID. Enter -1 to stop adding Discounts: ");

				choice = Integer.parseInt(reader.readLine());
				if(choice == -1){
					break;
				}
				DBNinja.usePizzaDiscount(ret, new Discount(choice, "Dummy", 0.0, false));
			}while(choice != -1);
		}

		return ret;
	}
	
	
	public static void PrintReports() throws SQLException, NumberFormatException, IOException
	{

		String line="";
		do{
		System.out.println("Which report do you wish to print? Enter\n(a) ToppingPopularity\n(b) ProfitByPizza\n(c) ProfitByOrderType:");
		line = reader.readLine();
		if(!(line.equals("a") || line.equals("b") || line.equals("c"))){
			System.out.println("I don't understand that input... returning to menu...");
		}
		}while(!(line.equals("a") || line.equals("b") || line.equals("c")));

		if(line.equals("a")){
			DBNinja.printToppingPopReport();
		}
		if(line.equals("b")){
			DBNinja.printProfitByPizzaReport();
		}
		if(line.equals("c")){
			DBNinja.printProfitByOrderType();
		}

	}

	//Prompt - NO CODE SHOULD TAKE PLACE BELOW THIS LINE
	// DO NOT EDIT ANYTHING BELOW HERE, THIS IS NEEDED TESTING.
	// IF YOU EDIT SOMETHING BELOW, IT BREAKS THE AUTOGRADER WHICH MEANS YOUR GRADE WILL BE A 0 (zero)!!

	public static void PrintMenu() {
		System.out.println("\n\nPlease enter a menu option:");
		System.out.println("1. Enter a new order");
		System.out.println("2. View Customers ");
		System.out.println("3. Enter a new Customer ");
		System.out.println("4. View orders");
		System.out.println("5. Mark an order as completed");
		System.out.println("6. View Inventory Levels");
		System.out.println("7. Add Inventory");
		System.out.println("8. View Reports");
		System.out.println("9. Exit\n\n");
		System.out.println("Enter your option: ");
	}

	/*
	 * autograder controls....do not modiify!
	 */

	public final static String autograder_seed = "6f1b7ea9aac470402d48f7916ea6a010";

	
	private static void autograder_compilation_check() {

		try {
			Order o = null;
			Pizza p = null;
			Topping t = null;
			Discount d = null;
			Customer c = null;
			ArrayList<Order> alo = null;
			ArrayList<Discount> ald = null;
			ArrayList<Customer> alc = null;
			ArrayList<Topping> alt = null;
			double v = 0.0;
			String s = "";

			DBNinja.addOrder(o);
			DBNinja.addPizza(p);
			DBNinja.useTopping(p, t, false);
			DBNinja.usePizzaDiscount(p, d);
			DBNinja.useOrderDiscount(o, d);
			DBNinja.addCustomer(c);
			DBNinja.completeOrder(o);
			alo = DBNinja.getOrders(false);
			o = DBNinja.getLastOrder();
			alo = DBNinja.getOrdersByDate("01/01/1999");
			ald = DBNinja.getDiscountList();
			d = DBNinja.findDiscountByName("Discount");
			alc = DBNinja.getCustomerList();
			c = DBNinja.findCustomerByPhone("0000000000");
			alt = DBNinja.getToppingList();
			t = DBNinja.findToppingByName("Topping");
			DBNinja.addToInventory(t, 1000.0);
			v = DBNinja.getBaseCustPrice("size", "crust");
			v = DBNinja.getBaseBusPrice("size", "crust");
			DBNinja.printInventory();
			DBNinja.printToppingPopReport();
			DBNinja.printProfitByPizzaReport();
			DBNinja.printProfitByOrderType();
			s = DBNinja.getCustomerName(0);
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
	}


}


