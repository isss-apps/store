package org.foobarter.isss.store;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

import org.foobarter.isss.store.model.client.ClientOrder;
import org.foobarter.isss.store.model.order.Customer;
import org.foobarter.isss.store.model.order.Order;
import org.foobarter.isss.store.model.order.OrderItem;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.UUID;

@Component
public class OrderItemAggregationStrategy implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

		Order order;
		if (oldExchange == null) {
			order = new Order();

			ClientOrder clientOrder = newExchange.getIn().getHeader("clientOrder", ClientOrder.class);

			Customer customer = new Customer();
			customer.setName(clientOrder.getName());
			customer.setAddress(clientOrder.getAddress());

			order.setCustomer(customer);
			order.setUuid(UUID.randomUUID().toString());

			order.setItems(new LinkedList<>());

			oldExchange = newExchange;
		}
		else {
			order = oldExchange.getIn().getBody(Order.class);
		}

		order.getItems().add(newExchange.getIn().getBody(OrderItem.class));

		oldExchange.getOut().setBody(order);

		return oldExchange;
	}
}
