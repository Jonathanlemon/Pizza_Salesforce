CREATE SCHEMA PIZZERIA;
USE PIZZERIA;

CREATE TABLE base(
	BaseCrust varchar(30),
    BaseSize varchar(30),
    BasePrice float not null,
    BaseCost float not null,
    primary key(BaseCrust, BaseSize)
);

#Stored procedure will be used to handle placing delivery orders to ensure customer has a valid address, city, state, and zip
#This cannot be easily done using SQL constraints
CREATE TABLE customer(
	CustomerID int auto_increment primary key,
    CustomerFirstName varchar(30) not null,
    CustomerLastName varchar(30) not null,
    CustomerAreaCode int not null,
    CustomerPhone int not null,
    CustomerAddress varchar(30),
    CustomerCity varchar(30),
    CustomerState varchar(2),
    CustomerZip int
);

CREATE TABLE anOrder(
	AnOrderID int auto_increment primary key,
	AnOrderType varchar(30) not null,
    AnOrderState bool not null default 0,
    AnOrderPrice float not null,
    AnOrderCost float not null,
    AnOrderTime timestamp not null default NOW(),
    AnOrderTable int,
    AnOrderPickedUp bool,
    AnOrderCustomerID int not null default 1,
    foreign key(AnOrderCustomerID) references customer(CustomerID) on delete cascade,
	constraint check(AnOrderType = "dinein" or AnOrderType = "pickup" or AnOrderType = "delivery"),
    constraint check((AnOrderType = "dinein" and AnOrderTable is not NULL) or ((AnOrderType = "pickup" or AnOrderType = "delivery") and AnOrderCustomerID is not null))
);

CREATE TABLE pizza(
	PizzaID int auto_increment primary key,
    PizzaAnOrderID int not null,
    PizzaCrust varchar(30) not null,
    PizzaSize varchar(30) not null,
    PizzaState bool not null,
    PizzaPrice float not null,
    PizzaCost float not null,
    foreign key(PizzaAnOrderID) references anOrder(AnOrderID) on delete cascade,
    foreign key(PizzaCrust, PizzaSize) references base(BaseCrust, BaseSize) on delete cascade
);

CREATE TABLE discount(
	DiscountID int primary key auto_increment,
    DiscountName varchar(30),
    DiscountPercentOff float,
    DiscountDollarOff float,
    constraint check(DiscountPercentOff is not null or DiscountDollarOff is not null),
    constraint check(DiscountPercentOff is null or DiscountDollarOff is null),
    Unique(DiscountName)
);

CREATE TABLE pizza_to_discount(
	PizzaID int,
    DiscountID int,
    primary key(PizzaID, DiscountID),
    foreign key(PizzaID) references pizza(PizzaID) on delete cascade,
    foreign key(DiscountID) references discount(DiscountID) on delete cascade
);

CREATE TABLE order_to_discount(
	AnOrderID int,
    DiscountID int,
    primary key(AnOrderID, DiscountID),
    foreign key(AnOrderID) references anOrder(AnOrderID) on delete cascade,
    foreign key(DiscountID) references discount(DiscountID) on delete cascade
);

CREATE TABLE topping(
	ToppingID int primary key auto_increment,
    ToppingName varchar(30),
    ToppingPrice float not null,
    ToppingCost float not null,
	ToppingCurInv int not null,
    ToppingMinInv int not null
);

CREATE TABLE topping_to_serving(
	ToppingID int,
    Size varchar(30),
    Serving float,
    primary key(ToppingID, Size),
    foreign key(ToppingID) references topping(ToppingID) on delete cascade
);

CREATE TABLE pizza_to_topping(
	PizzaID int,
    ToppingID int,
    Amount int default 1,
    primary key(PizzaID, ToppingID),
    foreign key(PizzaID) references pizza(PizzaID) on delete cascade,
    foreign key(ToppingID) references topping(ToppingID) on delete cascade
);