# camel-example-spring-boot-geocoder2
Apache CamelのSpring Bootインテグレーションサンプル（GeoCode）その２

先日のCamel In Actionの読書会の席で、「やるなら最新バージョンでしょっ」とコメント頂きましたので、今回はCamel最新バージョン(2.20.1)＋camel-http4でやってみます。

## のっけから、camel-http4優秀です！
んと、ドキュメント見ると、初めからシステム・プロパティのサポートが実装されています！すばらすぃいー。

[Apache Camel > Documentation > Components > HTTP4から抜粋](http://camel.apache.org/http4.html)
```
Using System Properties
When useSystemProperties=true the camel-http4 client can make use the following system properties:
・java.home
・javax.net.ssl.trustStoreType
・javax.net.ssl.trustStore
・javax.net.ssl.trustStoreProvider
・javax.net.ssl.trustStorePassword
・javax.net.ssl.keyStore
・javax.net.ssl.keyStoreProvider
・javax.net.ssl.keyStorePassword
・javax.net.ssl.keyStoreType
・http.proxyHost
・http.proxyPort
・http.nonProxyHosts
・http.keepAlive
・http.maxConnections
・ssl.KeyManagerFactory.algorithm
・ssl.TrustManagerFactory.algorithm
```

これで全て解決ですね。。。と思ったのですが、
1. 「useSystemProperties」は、デフォルトfalse。
2. やっぱりGeocoderは、別物。

のようです。1.は、デフォルトtrueで良いように思うのですが、やっぱcamel-httpからの挙動を維持するためなんですかね。まぁ、Proxy越えさせる方がマイノリティなんですかね。
ということで、「useSystemProperties」の設定を一元管理することと、Proxy関連設定をSpring Bootのapplication.propertiesで設定して、本パラメータを元にGeocoderにも適用できるようにしてみようかと。

## 例によって、Spring BootのConfigurationを実装する。
ということで、デフォルトのAutoConfigurationを上書くようなConfigurationを実装してみます。
前提として、application.propertiesには、
```java
camel.component.http4.ext.use-system-properties=true
camel.component.http4.ext.proxy-host=133.199.251.110
camel.component.http4.ext.proxy-port=8080
camel.component.http4.ext.non-proxy-hosts=localhost|10.51.*.*
```
という設定を記載し、「useSystemProperties」がtrueで、上記ProxyHost等が設定されている場合は、こちらをシステム・プロパティに設定するようなことをやってみようかと。これは以下のような感じ。（あいかわらず、HTTPSとProxy認証は手抜いてます）
```java
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
```
で、前と同じProxy関連のプロパティと、useSystemPropertiesを持つConfigurationを以下のように実装。
```java
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
  // 以下省略
```
あいかわらずHttpEndPointの生成箇所はフックできなかったので、HttpComponent生成時に拡張して、指定されたuseSystemPropertiesによって設定する。この際、URIに既にuseSystemPropertiesが存在する場合は、最強として大人の対応。Geocoder側は、基本前と同じやり方でOK。

## やっぱり。。。
新しいコンポーネントは優秀ですね。デフォルトの方向は致し方なしですが、初めからこっち使っていれば、そんなに悩むこともなかったかも。今日は以上。