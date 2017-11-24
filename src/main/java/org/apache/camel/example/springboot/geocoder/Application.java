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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// CHECKSTYLE:OFF
@SpringBootApplication
public class Application {

	/**
	 * Main method to start the application.
	 */
	public static void main(String[] args) {
		// FIXME これも無効
		System.setProperty("http.proxyHost", "133.199.251.110");
		System.setProperty("http.proxyPort", "8080");
		System.setProperty("http.nonProxyHosts", "localhost|10.51.219.43");
		System.setProperty("https.proxyHost", "133.199.251.110");
		System.setProperty("https.proxyPort", "8080");

		SpringApplication.run(Application.class, args);
	}

}
// CHECKSTYLE:ON