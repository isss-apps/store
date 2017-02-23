package org.foobarter.isss.store.model.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientCatalogEntry {
	private long id;
	private String name;
	private BigDecimal price;

	private boolean isDir;

	private Long rootCategory;

	public ClientCatalogEntry() {}

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

	@Override
	public String toString() {
		return "ClientCatalogEntry{" +
				"id=" + id +
				", name='" + name + '\'' +
				", price=" + price +
				", isDir=" + isDir +
				", rootCategory=" + rootCategory +
				'}';
	}
}
