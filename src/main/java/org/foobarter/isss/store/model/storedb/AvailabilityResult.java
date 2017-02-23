package org.foobarter.isss.store.model.storedb;

public class AvailabilityResult {
	private long id;
	private long stock;
	private int supplier_days;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getStock() {
		return stock;
	}

	public void setStock(long stock) {
		this.stock = stock;
	}

	public int getSupplier_days() {
		return supplier_days;
	}

	public void setSupplier_days(int supplier_days) {
		this.supplier_days = supplier_days;
	}
}
