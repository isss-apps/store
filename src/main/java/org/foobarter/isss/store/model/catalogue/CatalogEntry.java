package org.foobarter.isss.store.model.catalogue;

import java.math.BigDecimal;

public class CatalogEntry {
	@Override
	public String toString() {
		return "CatalogEntry{" +
				"storeId=" + storeId +
				", id=" + id +
				", name='" + name + '\'' +
				", price=" + price +
				", isDir=" + isDir +
				", rootCategory=" + rootCategory +
				'}';
	}

	private Long storeId;

	public CatalogEntry() {}

	public Long getStoreId() {
		return storeId;
	}

	public void setStoreId(Long storeId) {
		this.storeId = storeId;
	}

	private long id;
	private String name;
	private BigDecimal price;

	private boolean isDir;

	private Long rootCategory;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public boolean isDir() {
		return isDir;
	}

	public void setDir(boolean dir) {
		isDir = dir;
	}

	public Long getRootCategory() {
		return rootCategory;
	}

	public void setRootCategory(Long rootCategory) {
		this.rootCategory = rootCategory;
	}
}
