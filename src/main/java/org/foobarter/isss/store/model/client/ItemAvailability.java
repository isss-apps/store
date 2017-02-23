package org.foobarter.isss.store.model.client;

public class ItemAvailability {
	private long itemId;
	boolean inStock;
	int supplierDays;

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long itemId) {
		this.itemId = itemId;
	}

	public boolean isInStock() {
		return inStock;
	}

	public void setInStock(boolean inStock) {
		this.inStock = inStock;
	}

	public int getSupplierDays() {
		return supplierDays;
	}

	public void setSupplierDays(int supplierDays) {
		this.supplierDays = supplierDays;
	}
}
