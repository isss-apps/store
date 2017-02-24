package org.foobarter.isss.store.model.order;

import java.util.List;

public class Order {
	private String uuid;
	private Customer customer;
	private List<OrderItem> items;

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

	@Override
	public String toString() {
		return "Order{" +
				"uuid='" + uuid + '\'' +
				", customer=" + customer +
				", items=" + items +
				'}';
	}
}
