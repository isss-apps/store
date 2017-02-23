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

		from("direct:catalog-get-request")
				.to("direct:clean-http-headers")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, header("catalog-query-path"))
				.removeHeader("id")
				.removeHeader("catalog-query-path")
				.to("jetty:http://catalog.foobarter.org:8080");

		from("direct:catalogList")
				.setHeader("catalog-query-path", simple("/entries/list/${header.id}"))
				.to("direct:catalog-get-request")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:catalogRoot")
				.setHeader("catalog-query-path", constant("/entries/list"))
				.to("direct:catalog-get-request")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:availability")
				.to("direct:clean-http-headers")
				.setHeader("catalog-query-path", simple("/entries/${header.id}"))
				.to("direct:catalog-get-request")
				.unmarshal().json(JsonLibrary.Jackson, CatalogEntry.class)

				// store catalog in the headers for further processing
				.setHeader("catalog", body())

				.choice()
					.when(simple("${header.catalog.storeId} != null"))
						.to("direct:storedb-query")
					.otherwise()
						.to("direct:notavailable");

		from("direct:storedb-query")
				.to("sql:select id, stock, supplier_days from items where id = :#${header.catalog.storeId}?" +
						"dataSource=dataSource&" +
						"outputType=SelectOne&" +
						"outputClass=org.foobarter.isss.store.model.storedb.AvailabilityResult")
				.choice()
					.when(simple("${header.CamelSqlRowCount} == 0"))
						.to("direct:notavailable")
					.otherwise()
						.process(itemAvailabilityProcessor)
				.endChoice();

		from("direct:notavailable")
				.setHeader("CamelHttpResponseCode", simple("404"))
				.setFaultBody(constant("This item is not available anymore, sorry for inconvenience!"));

	}
}
