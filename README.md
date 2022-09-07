# hazelcast-ssl
hazelcast-ssl extension for HazelcastIMDG 4.2.2

To use ssl mode, member should have ssl section configured by this way:

```yaml
hazelcast:
  network:
    ssl:
      enabled: true
      factory-class-name: com.hazelcast.ssl.SimpleSSLContextFactory
      properties:
        protocol: TLSv1.3
        mutualAuthentication: OPTIONAL
        keyStore: /path/to/server/keystore.p12
        keyStorePassword: keystorePassword
        keyStoreType: PKCS12
        trustStore: /path/to/truststore.jks
        trustStorePassword: truststorePassword
        trustStoreType: PKCS12
        validateIdentity: false
        ciphersuites: ciphersuites-comma-separated
```

Client should be also configured like this, except keyStore and trustStore directives. 
It should have its own files.

It may look like this when using Java:

```java
public HazelcastInstance hazelcastInstance(ClientConfig clientConfig){
    var sslConfig=new SSLConfig();
    sslConfig.setEnabled(sslEnabled);

    var sslProperties=new Properties();
    sslProperties.setProperty("protocol",protocol);
    sslProperties.setProperty("mutualAuthentication",mutualAuthentication);
    sslProperties.setProperty("keyStore",keyStore);
    sslProperties.setProperty("keyStorePassword",keyStorePassword);
    sslProperties.setProperty("keyStoreType",keyStoreType);
    sslProperties.setProperty("trustStore",trustStore);
    sslProperties.setProperty("trustStorePassword",trustStorePassword);
    sslProperties.setProperty("trustStoreType",trustStoreType);
    sslProperties.setProperty("validateIdentity","true");
    sslProperties.setProperty("ciphersuites","ciphersuites-comma-separated");

    sslConfig.setProperties(sslProperties);
    sslConfig.setFactoryClassName(factoryClassName);

    clientConfig.getNetworkConfig().setSSLConfig(sslConfig);

    HazelcastInstance client=HazelcastClient.newHazelcastClient(clientConfig);
}
```

Oprosso team ©
