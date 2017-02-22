package org.foobarter.isss.store;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

import org.foobarter.isss.store.model.client.ItemAvailability;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpMethod;

@SpringBootApplication
public class StoreRoute extends RouteBuilder {

	// must have a main method spring-boot can run
	public static void main(String[] args) {
		SpringApplication.run(StoreRoute.class, args);
	}

    @Override
    public void configure() throws Exception {
		restConfiguration().component("jetty").host("0.0.0.0").port(8080).bindingMode(RestBindingMode.auto);

		rest("/availability")
				.produces("application/json")
				.get("/{id}").outType(ItemAvailability.class)
					.to("direct:availability");

		from("direct:availability")
				.removeHeaders("CamelHttp*")
				.removeHeader("Host")
				.removeHeader("CamelServletContextPath")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.setHeader(Exchange.CONTENT_TYPE, simple("application/json"))
				.setHeader(Exchange.HTTP_PATH, simple("/entries/${header.id}"))
				.removeHeader("id")
				.to("log:org.foobarter.isss.store?level=INFO&showAll=true")
				.to("jetty:http://catalog.foobarter.org:8080")
				.log("${body}");
		// TODO: sql query and merge and return

	}
}
