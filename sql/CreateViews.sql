Use PIZZERIA;
drop view if exists ToppingPopularity;
drop view if exists ProfitByPizza;
drop view if exists ProfitByOrderType;

Create VIEW ToppingPopularity as select topping.ToppingName as "Topping", IFNULL(SUM(Amount), 0) as "ToppingCount" from topping left outer join pizza_to_topping on topping.ToppingID = pizza_to_topping.ToppingID group by topping.ToppingName order by SUM(Amount) desc;
select * from ToppingPopularity;

Create VIEW ProfitByPizza as select PizzaSize as "Size", PizzaCrust as "Crust", Round(Sum(PizzaPrice - PizzaCost), 2) as "Profit", CONCAT(MONTH(AnOrderTime),"/", YEAR(AnOrderTime)) as "Order Month" from pizza join anOrder on pizza.PizzaAnOrderID = anOrder.AnOrderID group by PizzaSize, PizzaCrust order by  Round(Sum(PizzaPrice - PizzaCost), 2) desc;
select * from ProfitByPizza;

Create VIEW ProfitByOrderType as select AnOrderType as "customerType", CONCAT(MONTH(AnOrderTime),"/", YEAR(AnOrderTime))  as "Order Month", Round(Sum(AnOrderPrice), 2) as "TotalOrderPrice", Round(Sum(AnOrderCost), 2) as "TotalOrderCost", Round(Sum(AnOrderPrice - AnOrderCost), 2) as "Profit" from anOrder group by anOrderType, CONCAT(MONTH(AnOrderTime),"/", YEAR(AnOrderTime)) union select NULL, "Grand Total", Round(Sum(AnOrderPrice), 2),Round(Sum(AnOrderCost), 2), Round(SUM(AnOrderPrice - AnOrderCost), 2) from anOrder order by field(customerType, "delivery", "pickup", "dinein") desc, Profit desc;
select * from ProfitByOrderType;