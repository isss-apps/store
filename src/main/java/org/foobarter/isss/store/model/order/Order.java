package org.foobarter.isss.store.model.order;

import java.math.BigDecimal;
import java.util.List;

public class Order {
	private String uuid;
	private Customer customer;
	private List<OrderItem> items;
	private BigDecimal price = new BigDecimal(0);

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public void setItems(List<OrderItem> items) {
		this.items = items;
	}

	public void updatePrice(BigDecimal price) {
		this.price = this.price.add(price);
	}

	@Override
	public String toString() {
		return "Order{" +
				"uuid='" + uuid + '\'' +
				", customer=" + customer +
				", items=" + items +
				'}';
	}
}
