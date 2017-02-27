package org.foobarter.isss.store;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;

import org.foobarter.isss.store.model.catalogue.CatalogEntry;
import org.foobarter.isss.store.model.client.ClientCatalogEntry;
import org.foobarter.isss.store.model.client.ClientOrder;
import org.foobarter.isss.store.model.client.ClientOrderItem;
import org.foobarter.isss.store.model.client.ItemAvailability;
import org.foobarter.isss.store.model.order.Order;
import org.foobarter.isss.store.model.order.OrderItem;
import org.foobarter.isss.store.model.order.OrderReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.concurrent.RejectedExecutionException;

@SpringBootApplication
public class StoreRoute extends RouteBuilder {

	// must have a main method spring-boot can run
	public static void main(String[] args) {
		SpringApplication.run(StoreRoute.class, args);
	}

	@Autowired
	private ItemAvailabilityProcessor itemAvailabilityProcessor;

	@Autowired
	private NoHeaderStrategy noHeaderStrategy;

	@Autowired
	private OrderItemAggregationStrategy orderItemAggregationStrategy;


	@Value("${service.store.order.url}")
	private String storeOrderServiceUrl;

	@Value("${service.store.catalog.url}")
	private String storeCatalogServiceUrl;

    @Override
    public void configure() throws Exception {

    	getContext().getRegistry().lookupByName("noHeaderStrategy").toString();

		restConfiguration().component("jetty").host("0.0.0.0").port(8080).bindingMode(RestBindingMode.auto);

		final JacksonDataFormat clientCatalogEntriesListDataFormat = new JacksonDataFormat();
		clientCatalogEntriesListDataFormat.useList();
		clientCatalogEntriesListDataFormat.setUnmarshalType(ClientCatalogEntry.class);

		rest("/")
				.get("/")
					.route()
						.process(x -> {
							x.getOut().setHeader(Exchange.CONTENT_TYPE, "text/html");
							x.getOut().setBody(StoreRoute.class.getResourceAsStream("/index.html"));
						})
					.endRest()
				.get("/jquery.js")
					.route()
						.process(x -> {
							x.getOut().setHeader(Exchange.CONTENT_TYPE, "text/javascript");
							x.getOut().setBody(StoreRoute.class.getResourceAsStream("/jquery.js"));
						});

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

		rest("/order")
				.consumes("application/json")
				.produces("application/json")
				.put().type(ClientOrder.class).outType(OrderReceipt.class)
					.to("direct:order");


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
				.to("log:jetty-request?level=INFO&showAll=true&multiline=true&showStreams=true")
				.to("jetty:" + storeCatalogServiceUrl + "?headerFilterStrategy=noHeaderStrategy");

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

				.to("log:catalog?level=INFO&showAll=true&multiline=true")

				.choice()
					.when(simple("${body.storeId} == ''"))
						.to("direct:notavailable")
					.otherwise()
						.to("direct:storedb-query")
						//.to("direct:storedb-query-with-circuit-breaker")

				.endChoice();

		from("direct:storedb-query-with-circuit-breaker")
			.onException(RejectedExecutionException.class)
				.handled(true)
					.setHeader("CamelHttpResponseCode", simple("503"))
					.setBody(constant("Store DB has been too busy, giving her a rest, availability and delivery dates not currently available."))
			.end()
				.loadBalance()
					.circuitBreaker(2, 30_000L, DataAccessResourceFailureException.class)
					.to("direct:storedb-query")
				.end();

		from("direct:storedb-query")

				// POI: timeout
				.onException(DataAccessResourceFailureException.class)
					.handled(true)
					.setHeader("CamelHttpResponseCode", simple("503"))
					.setBody(constant("Store DB takes a bit too much time, availability and delivery dates not currently available."))
				.end()

				.to("sql:select id, stock, supplier_days from store_slow where id = :#${header.catalog.storeId}?" +
						"dataSource=dataSource&" +
						"outputType=SelectOne&" +
						"outputClass=org.foobarter.isss.store.model.storedb.AvailabilityResult&" +
						"template.queryTimeout=5") // POI: timeout

				.to("log:db?level=INFO&showAll=true&multiline=true")

				.choice()
					.when(simple("${header.CamelSqlRowCount} == 0"))
						.to("direct:notavailable")
					.otherwise()
						.process(itemAvailabilityProcessor)
				.endChoice();

		from("direct:notavailable")
				.setHeader("CamelHttpResponseCode", simple("404"))
				.setBody(constant("This item is not available anymore, sorry for inconvenience!"));

		from("direct:order")
				.to("log:order?level=INFO&showAll=true&multiline=true")
				.setHeader("clientOrder", body())
				//.setHeader("items", simple("${body.items}"))
				.to("log:header?level=INFO&showAll=true&multiline=true")
				.split(simple("${body.items}"), orderItemAggregationStrategy)
					.to("log:item?level=INFO&showAll=true&multiline=true")
					.setHeader("catalog-query-path", simple("/entries/${body.catalogId}"))
					.setHeader("clientOrderItem", body())
					.setBody(constant(null))
					.to("direct:catalog-get-request")
					.unmarshal().json(JsonLibrary.Jackson, CatalogEntry.class)
					.to("log:response?level=INFO&showAll=true&multiline=true")
					.process(x -> {
						ClientOrderItem clientOrderItem = x.getIn().getHeader("clientOrderItem", ClientOrderItem.class);
						CatalogEntry catalogEntry = x.getIn().getBody(CatalogEntry.class);

						OrderItem orderItem = new OrderItem();

						orderItem.setAmount(clientOrderItem.getAmount());
						orderItem.setItemPrice(catalogEntry.getPrice());
						orderItem.setStoreId(catalogEntry.getStoreId());

						x.getOut().setHeaders(x.getIn().getHeaders());
						x.getOut().setBody(orderItem);
					})
				.to("log:processed?level=INFO&showAll=true&multiline=true")
				.end()
				.to("log:aggregated?level=INFO&showAll=true&multiline=true")

				.to("direct:clean-http-headers")
				.setHeader(Exchange.HTTP_METHOD, constant("PUT"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, constant("/order"))

				.marshal().json(JsonLibrary.Jackson, Order.class)
				.to("jetty:" + storeOrderServiceUrl + "?headerFilterStrategy=noHeaderStrategy")
				.to("log:receipt?level=INFO&showAll=true&multiline=true")
				.unmarshal().json(JsonLibrary.Jackson, OrderReceipt.class);

	}
}
