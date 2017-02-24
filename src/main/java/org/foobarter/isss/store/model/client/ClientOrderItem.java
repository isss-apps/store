package org.foobarter.isss.store.model.client;

public class ClientOrderItem {
	private long catalogId;
	private int amount;

	public long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(long catalogId) {
		this.catalogId = catalogId;
	}

	@Override
	public String toString() {
		return "ClientOrderItem{" +
				"catalogId=" + catalogId +
				", amount=" + amount +
				'}';
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
}
