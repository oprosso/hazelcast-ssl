# hazelcast-ssl
SSL extension for HazelcastIMDG 4.2.2

Maven
```xml
<!-- https://mvnrepository.com/artifact/ru.oprosso/hazelcast-ssl -->
<dependency>
    <groupId>ru.oprosso</groupId>
    <artifactId>hazelcast-ssl</artifactId>
    <version>1.0.12</version>
</dependency>
```

Gradle (Kotlin)
```kt
implementation("ru.oprosso:hazelcast-ssl:1.0.12")
```

Gradle(short)
```groovy
implementation 'ru.oprosso:hazelcast-ssl:1.0.12'
```

To build jar, execute command

`./gradlew clean build`

in project root.

Jar file will appear in build/lib folder. To connect it to haselcast member instance, set its path to hazelcast member CLASSPATH env variable.
When running hazelcast member, need use our plugin's launcher: `com.hazelcast.CustomHazelcastMemberStarter` 
There is `start-hazelcast.sh`, which with you can replace original one inside member image or container.
It accepts MAIN_CLASS env variable, so you can pass `MAIN_CLASS=com.hazelcast.CustomHazelcastMemberStarter` to it.

E.g.: 
```bash
JAVA_OPTS="-Dhazelcast.config=/opt/hazelcast/configs/hazelcast-1.yaml" MAIN_CLASS=com.hazelcast.CustomHazelcastMemberStarter sh start-hazelcast.sh
```

or inside docker-compose.yaml:
```yaml
...
  hazelcast-1:
    image: hazelcast:4.2.2
    restart: unless-stopped
    environment:
      JAVA_OPTS: -Dhazelcast.config=/opt/hazelcast/configs/hazelcast-1.yaml
      MAIN_CLASS: com.hazelcast.CustomHazelcastMemberStarter
    network_mode: host
  volumes:
    - ./my-hazelcast-configs:/opt/hazelcast/configs
```


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
