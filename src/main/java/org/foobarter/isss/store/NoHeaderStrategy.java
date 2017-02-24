package org.foobarter.isss.store;

import org.apache.camel.Exchange;

import org.springframework.stereotype.Component;

@Component
public class NoHeaderStrategy implements org.apache.camel.spi.HeaderFilterStrategy {
	@Override
	public boolean applyFilterToCamelHeaders(String headerName, Object headerValue, Exchange exchange) {
		return true;
	}

	@Override
	public boolean applyFilterToExternalHeaders(String headerName, Object headerValue, Exchange exchange) {
		return true;
	}
}
