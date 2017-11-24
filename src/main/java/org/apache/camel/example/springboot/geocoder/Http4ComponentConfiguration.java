/**
 *
 */
package org.apache.camel.example.springboot.geocoder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.geocoder.GeoCoderComponent;
import org.apache.camel.component.geocoder.GeoCoderEndpoint;
import org.apache.camel.component.geocoder.springboot.GeoCoderComponentAutoConfiguration;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.http4.HttpEndpoint;
import org.apache.camel.component.http4.springboot.HttpComponentConfiguration;
import org.apache.camel.spi.ComponentCustomizer;
import org.apache.camel.spi.HasId;
import org.apache.camel.spring.boot.util.CamelPropertiesHelper;
import org.apache.camel.spring.boot.util.HierarchicalPropertiesEvaluator;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.ObjectHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.code.geocoder.Geocoder;

/**
 * @author w2327eng
 *
 */
@Configuration
@ConfigurationProperties(prefix = "camel.component.http4.ext")
public class Http4ComponentConfiguration {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private CamelContext camelContext;

	@Autowired
	private HttpComponentConfiguration configuration;

	@Autowired(required = false)
	private List<ComponentCustomizer<HttpComponent>> customizers;

	/**
	 * Proxy host for Camel Http component.
	 */
	private String proxyHost;
	/**
	 * Proxy port for Camel Http component.
	 */
	private String proxyPort;
	/**
	 * non-Proxy hosts for Camel Http component.
	 */
	private String nonProxyHosts;

	private boolean useSystemProperties;

	/**
	 * Proxy host getter.
	 * @return proxyHost
	 */
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * Proxy host setter.
	 * @param proxyHost proxyHost
	 */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * Proxy port getter.
	 * @return proxyPort
	 */
	public String getProxyPort() {
		return proxyPort;
	}

	/**
	 * Proxy port setter.
	 * @param proxyPort proxyPort
	 */
	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}
	/**
	 * non-Proxy hosts getter.
	 * @return nonProxyHosts
	 */
	public String getNonProxyHosts() {
		return nonProxyHosts;
	}

	/**
	 * non-Proxy hosts setter.
	 * @param nonProxyHosts non-Proxy hosts
	 */
	public void setNonProxyHosts(String nonProxyHosts) {
		this.nonProxyHosts = nonProxyHosts;
	}

	/**
	 * @return useSystemProperties
	 */
	public boolean getUseSystemProperties() {
		return useSystemProperties;
	}

	/**
	 * @param useSystemProperties
	 *            セットする useSystemProperties
	 */
	public void setUseSystemProperties(boolean useSystemProperties) {
		this.useSystemProperties = useSystemProperties;
	}

	private boolean isAlreadySet;

	private void setUpSystemProperties() {
		if(!isAlreadySet) {
			if(getUseSystemProperties()) {
				if(StringUtils.isNotBlank(getProxyHost()) && StringUtils.isNotBlank(getProxyPort())) {
					System.setProperty("http.proxyHost", getProxyHost());
					System.setProperty("http.proxyPort", getProxyPort());
					System.setProperty("http.nonProxyHosts", getNonProxyHosts());
				} else {
					setProxyHost(System.getProperty("http.proxyHost"));
					setProxyPort(System.getProperty("http.proxyPort"));
					setNonProxyHosts(System.getProperty("http.nonProxyHosts"));
				}
				// LOG INFO
				logger.info("Setting up Proxy for system properties. host = {}, port = {}, nonProxyHosts = {}"
						, getProxyHost()
						, getProxyPort()
						, getNonProxyHosts());
			}
			isAlreadySet = true;
		}
	}

	@Bean(name = { "http4-component", "https4-component" })
	public HttpComponent configureHttpComponent() throws Exception {
		setUpSystemProperties();
		final HttpComponent component = new HttpComponent() {
			@Override
			protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
				final HttpEndpoint endPoint = (HttpEndpoint)super.createEndpoint(uri, remaining, parameters);
				if(getUseSystemProperties() && !uri.contains("useSystemProperties")) {
					endPoint.setUseSystemProperties(true);
				}
				return endPoint;
			}
		};
		component.setCamelContext(camelContext);
		final Map<String, Object> parameters = new HashMap<>();
		IntrospectionSupport.getProperties(configuration, parameters, null, false);
		for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
			final Object value = entry.getValue();
			final Class<?> paramClass = value.getClass();
			if (paramClass.getName().endsWith("NestedConfiguration")) {
				final Class<?> nestedClass = (Class<?>) paramClass.getDeclaredField("CAMEL_NESTED_CLASS").get(null);
				final Map<String, Object> nestedParameters = new HashMap<>();
				IntrospectionSupport.getProperties(value, nestedParameters, null, false);
				final Object nestedProperty = nestedClass.newInstance();
				CamelPropertiesHelper.setCamelProperties(camelContext, nestedProperty, nestedParameters, false);
				entry.setValue(nestedProperty);
			}
		}
		CamelPropertiesHelper.setCamelProperties(camelContext, component, parameters, false);
		if (ObjectHelper.isNotEmpty(customizers)) {
			for (final ComponentCustomizer<HttpComponent> customizer : customizers) {
				final boolean useCustomizer = (customizer instanceof HasId)
						? HierarchicalPropertiesEvaluator.evaluate(applicationContext.getEnvironment(),
								"camel.component.customizer", "camel.component.http4.customizer",
								((HasId) customizer).getId())
						: HierarchicalPropertiesEvaluator.evaluate(applicationContext.getEnvironment(),
								"camel.component.customizer", "camel.component.http4.customizer");
				if (useCustomizer) {
					logger.debug("Configure component {}, with customizer {}", component, customizer);
					customizer.customize(component);
				}
			}
		}
		return component;
	}

	/**
	 * If settings exist, create a {@linkplain GeoCoderComponent} with proxy settings
	 * <br>This method override {@linkplain GeoCoderComponentAutoConfiguration#configureGeoCoderComponent(CamelContext)} method.
	 * @param camelContext Apache Camel Context instance.
	 * @return GeocoderComponent instance with already setting up proxies;
	 */
	@Bean(name = "geocoder-component")
	public GeoCoderComponent configureGeoCoderComponent(CamelContext camelContext) {
		setUpSystemProperties();
		final GeoCoderComponent component = new GeoCoderComponent(){
			/**
			 * If Proxy is not set with parameters for Route, Proxy of application setting is set as a parameter of Route.
			 * @see org.apache.camel.component.geocoder.GeoCoderComponent#createEndpoint(java.lang.String, java.lang.String, java.util.Map)
			 */
			@Override
			protected Endpoint createEndpoint(
					final String uri,
					final String remaining,
					final Map<String, Object> parameters) throws Exception {
				final GeoCoderEndpoint endPoint = (GeoCoderEndpoint)super.createEndpoint(uri, remaining, parameters);
				if(StringUtils.isNotBlank(getProxyHost()) && StringUtils.isNotBlank(getProxyPort())) {
					if(!uri.contains("proxyHost") && !uri.contains("proxyPort")) {
						// uriは、"/geocoder"なので、Geocoderから実際のホストを取得して判断
						if(needProxy("https://" + Geocoder.getGeocoderHost())) {
							endPoint.setProxyHost(getProxyHost());
							endPoint.setProxyPort(Integer.parseInt(getProxyPort()));
							// LOG INFO
							logger.info("Setting proxy for GeoCoderEndpoint. host = {}, port = {}", getProxyHost(), getProxyPort());
						}
					} else {
						// LOG INFO
						logger.info("GeoCoderEndpoint already set proxy. uri = {}", uri);
					}
				}
				return endPoint;
			}
		};
		component.setCamelContext(camelContext);
		return component;
	}

	/**
	 * 指定されたURIエンドポイントと{@linkplain #nonProxyHosts}を確認し、Proxy有無を返す。
	 * @param endPointUri URIエンドポイント
	 * @return Proxyが必要な場合、true
	 */
	private boolean needProxy(final String endPointUri) {
		boolean need = true;
		try{
			final String endPointHost = new URL(endPointUri).getHost();
			final Pattern nphPattern = getNonProxyPattern();
			if(nphPattern != null) {
				if(nphPattern.matcher(endPointHost).matches()) {
					// LOG INFO
					logger.info("EndPointUri match! nonProxyHost = {}, endPointUri = {}", nonProxyHosts, endPointUri);
					need = false;
				}
			}
		} catch (MalformedURLException e) {
			// LOG WARN
			logger.warn("Invalid EndPointUri {}", endPointUri, e);
		}
		return need;
	}

	private Pattern nonProxyHostsPattern;

	/**
	 * 以下から拝借。
	 * @see https://stackoverflow.com/questions/17615300/valid-regex-for-http-nonproxyhosts
	 * @param nonProxyHosts
	 * @return
	 */
	private Pattern getNonProxyPattern() {
		if(nonProxyHostsPattern != null) return nonProxyHostsPattern;
		if (StringUtils.isBlank(getNonProxyHosts())) return null;

		// "*.fedora-commons.org" -> ".*?\.fedora-commons\.org"
		String _nonProxyHosts = getNonProxyHosts().replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*?");

		// a|b|*.c -> (a)|(b)|(.*?\.c)
		_nonProxyHosts = "(" + _nonProxyHosts.replaceAll("\\|", ")|(") + ")";

		try {
			nonProxyHostsPattern = Pattern.compile(_nonProxyHosts);
		} catch (Exception e) {
			logger.error("Creating the nonProxyHosts pattern failed for http.nonProxyHosts=" + nonProxyHosts
					+ " with the following exception: " + e);
		}
		return nonProxyHostsPattern;
	}
}
