package org.foobarter.isss.store.model.order;

import java.math.BigDecimal;

public class OrderReceipt {
	private String message;
	private BigDecimal price;
	private boolean duplicate;

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}
}
