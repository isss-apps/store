package org.foobarter.isss.store.model.catalogue;

import org.foobarter.isss.store.model.client.ClientCatalogEntry;

public class CatalogEntry extends ClientCatalogEntry {
	private Long storeId;

	public CatalogEntry() {}

	public Long getStoreId() {
		return storeId;
	}

	public void setStoreId(Long storeId) {
		this.storeId = storeId;
	}
}
