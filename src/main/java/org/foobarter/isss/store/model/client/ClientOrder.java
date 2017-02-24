package org.foobarter.isss.store.model.client;

import java.util.List;

public class ClientOrder {
	private String name;
	private String address;

	private List<ClientOrderItem> items;

	@Override
	public String toString() {
		return "ClientOrder{" +
				"name='" + name + '\'' +
				", address='" + address + '\'' +
				", items=" + items +
				'}';
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public List<ClientOrderItem> getItems() {
		return items;
	}

	public void setItems(List<ClientOrderItem> items) {
		this.items = items;
	}
}
