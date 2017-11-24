/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.springboot.geocoder;

import static org.apache.camel.model.rest.RestParamType.query;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.google.code.geocoder.model.GeocodeResponse;

/**
 * A simple Camel REST DSL route example using the Geocoder component and documented with Swagger
 */
@Component
public class CamelGeocoderRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// rest-dsl is also configured in the application.properties file
		rest("/geocoder").description("Geocoder REST service")
			.consumes("application/json")
			.produces("application/json")

			.get().description("Geocoder address lookup").outType(GeocodeResponse.class)
				.param().name("address").type(query).description("The address to lookup").dataType("string").endParam()
				.responseMessage().code(200).message("Geocoder successful").endResponseMessage()
				// call the geocoder to lookup details from the provided address
				.toD("geocoder:address:${header.address}");
				// ここでパラメータとしてProxy指定が最強
				//.toD("geocoder:address:${header.address}?proxyHost=133.199.251.110&proxyPort=8080");

		// with Proxy
		from("timer://foo?period=5000")
			.to("https4://api.github.com/search/repositories?q=microservice")
			// ここでパラメータとしてProxy指定が最強
			//.to("https://api.github.com/search/repositories?q=microservice&proxyHost=133.199.251.110&proxyPort=8080")
			.to("file:{{env:HOMEPATH}}/Desktop/outBox?fileName=github_${date:now:yyyyMMdd_HHmmss}.json");

		// without Proxy
		from("timer://boo?period=5000")
			.process(exchange -> exchange.getIn().setBody("{\"woStatuses\": [\"Pending\"], \"project\": \"all\"}"))
			.setHeader(Exchange.HTTP_METHOD, constant("POST"))
			.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
				.to("http4://10.51.219.43:9080/webapi/wos")
				.to("file:{{env:HOMEPATH}}/Desktop/outBox?fileName=wos_${date:now:yyyyMMdd_HHmmss}.json");
	}
}