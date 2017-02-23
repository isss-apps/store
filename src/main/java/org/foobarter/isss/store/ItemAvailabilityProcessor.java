package org.foobarter.isss.store;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import org.foobarter.isss.store.model.catalogue.CatalogEntry;
import org.foobarter.isss.store.model.client.ItemAvailability;
import org.foobarter.isss.store.model.storedb.AvailabilityResult;
import org.springframework.stereotype.Component;

@Component
public class ItemAvailabilityProcessor implements Processor {
	@Override
	public void process(Exchange exchange) throws Exception {

		// result from the storedb
		AvailabilityResult result = exchange.getIn().getBody(AvailabilityResult.class);

		// we need to combine catalogue id and the AvailabilityResult:
		ItemAvailability itemAvailability = new ItemAvailability();

		itemAvailability.setItemId(exchange.getIn().getHeader("catalog", CatalogEntry.class).getId());
		itemAvailability.setInStock(result.getStock() != 0);
		itemAvailability.setSupplierDays(result.getSupplier_days());

		exchange.getOut().setBody(itemAvailability);
	}
}
