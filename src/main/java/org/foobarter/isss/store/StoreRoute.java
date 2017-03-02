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

	@Value("${rest.host}")
	private String host;

	@Value("${rest.port}")
	private int port;

	private void configureWebClient() throws Exception {
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
	}

    @Override
    public void configure() throws Exception {

		getContext().getShutdownStrategy().setTimeout(10);

		restConfiguration().component("jetty").host(host).port(port).bindingMode(RestBindingMode.auto);

		configureWebClient();

		final JacksonDataFormat clientCatalogEntriesListDataFormat = new JacksonDataFormat();
		clientCatalogEntriesListDataFormat.useList();
		clientCatalogEntriesListDataFormat.setUnmarshalType(ClientCatalogEntry.class);

		rest("/catalog")
				.produces("application/json")
				.get("/list/{id}").outTypeList(ClientCatalogEntry.class)
					.to("direct:catalogList")
				.get("/list").outTypeList(ClientCatalogEntry.class)
					.to("direct:catalogRoot");

		rest("/availability")
				.produces("application/json")
				.get("/{id}").outType(ItemAvailability.class)
					.to("direct:availability");

		rest("/order")
				.consumes("application/json")
				.produces("application/json")
				.put().type(ClientOrder.class).outType(OrderReceipt.class)
					.to("direct:order");

		from("direct:catalogList")
				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/list/${header.id}"))
				.setHeader(Exchange.HTTP_URI, constant(storeCatalogServiceUrl))
				.to("http4:somehost?headerFilterStrategy=noHeaderStrategy")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:catalogRoot")
				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, constant("/entries/list"))
				.setHeader(Exchange.HTTP_URI, constant(storeCatalogServiceUrl))
				.to("http4:somehost?headerFilterStrategy=noHeaderStrategy")
				.unmarshal(clientCatalogEntriesListDataFormat);

		from("direct:availability")
				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/${header.id}"))
				.setHeader(Exchange.HTTP_URI, constant(storeCatalogServiceUrl))
				.to("http4:somehost?headerFilterStrategy=noHeaderStrategy")

				.unmarshal().json(JsonLibrary.Jackson, CatalogEntry.class)

				// store catalog in the headers for further processing
				.setHeader("catalog", body())

				.choice()
					.when(simple("${body.storeId} == ''"))
						.to("direct:notavailable")
					.otherwise()
						.to("direct:storedb-query")

				.endChoice();

		from("direct:storedb-query")
				.to("sql:select id, stock, supplier_days from store_slow where id = :#${header.catalog.storeId}?" +
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
				.setBody(constant("This item is not available anymore, sorry for inconvenience!"));

		from("direct:order")
				.setHeader("clientOrder", body())
				.split(simple("${body.items}"), orderItemAggregationStrategy)
					.setHeader("clientOrderItem", body())
					.setBody(constant(null))

					.removeHeaders("CamelHttp*")
					.removeHeader("Host")
					.removeHeader("CamelServletContextPath")
					.setHeader(Exchange.HTTP_METHOD, constant("GET"))
					.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
					.setHeader(Exchange.HTTP_PATH, simple("/entries/${header.clientOrderItem.catalogId}"))
					.setHeader(Exchange.HTTP_URI, constant(storeCatalogServiceUrl))
					.to("http4:somehost?headerFilterStrategy=noHeaderStrategy")
					.unmarshal().json(JsonLibrary.Jackson, CatalogEntry.class)

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
					.end()

				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")

				.setHeader(Exchange.HTTP_METHOD, constant("PUT"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, constant("/order"))
				.setHeader(Exchange.HTTP_URI, constant(storeOrderServiceUrl))

				.marshal().json(JsonLibrary.Jackson, Order.class)

				.to("http4:somehost?headerFilterStrategy=noHeaderStrategy")

				.to("log:receipt?level=INFO&showAll=true&multiline=true")
				.unmarshal().json(JsonLibrary.Jackson, OrderReceipt.class);
	}
}
