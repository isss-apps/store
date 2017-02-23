package org.foobarter.isss.store;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

import org.foobarter.isss.store.model.catalogue.CatalogEntry;
import org.foobarter.isss.store.model.client.ClientCatalogEntry;
import org.foobarter.isss.store.model.client.ItemAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoreRoute extends RouteBuilder {

	// must have a main method spring-boot can run
	public static void main(String[] args) {
		SpringApplication.run(StoreRoute.class, args);
	}

	@Autowired
	private ItemAvailabilityProcessor itemAvailabilityProcessor;

    @Override
    public void configure() throws Exception {
		restConfiguration().component("jetty").host("0.0.0.0").port(8080).bindingMode(RestBindingMode.auto);

		final JacksonDataFormat clientCatalogEntriesListDataFormat = new JacksonDataFormat();
		clientCatalogEntriesListDataFormat.useList();
		clientCatalogEntriesListDataFormat.setUnmarshalType(ClientCatalogEntry.class);

		rest("/availability")
				.produces("application/json")
				.get("/{id}").outType(ItemAvailability.class)
					.to("direct:availability");

		rest("/catalog")
				.produces("application/json")
				.get("/list/{id}").outTypeList(ClientCatalogEntry.class)
					.to("direct:catalogList")
				.get("/list").outTypeList(ClientCatalogEntry.class)
					.to("direct:catalogRoot");

		from("direct:clean-http-headers")
				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")
				.end();

		from("direct:catalogList")
				.to("direct:clean-http-headers")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/list/${header.id}"))
				.removeHeader("id")
				.to("jetty:http://catalog.foobarter.org:8080")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:catalogRoot")
				.to("direct:clean-http-headers")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/list"))
				.to("jetty:http://catalog.foobarter.org:8080")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:availability")
				.to("direct:clean-http-headers")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/${header.id}"))
				.removeHeader("id")

				.to("jetty:http://catalog.foobarter.org:8080")
				.unmarshal().json(JsonLibrary.Jackson, CatalogEntry.class)

				// store catalog in the headers for further processing
				.setHeader("catalog", body())

				.to("sql:select id, stock, supplier_days from items where id = :#${header.catalog.storeId}?" +
						"dataSource=dataSource&" +
						"outputType=SelectOne&" +
						"outputClass=org.foobarter.isss.store.model.storedb.AvailabilityResult")

				.choice()
					.when(simple("${header.CamelSqlRowCount == 0}"))
						// TODO: some better response
						.setHeader("CamelHttpResponseCode", simple("404"))
						.setFaultBody(constant("This item is not available anymore, sorry for inconvenience!"))
					.otherwise()
						.process(itemAvailabilityProcessor)
				.endChoice();
	}
}
