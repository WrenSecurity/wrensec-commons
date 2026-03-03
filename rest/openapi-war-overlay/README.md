# openapi-war-overlay

This module packages [Swagger UI](https://github.com/swagger-api/swagger-ui)
assets as a WAR overlay. The Swagger UI distribution is pulled from the
`org.webjars:swagger-ui` WebJar at build time — version upgrades are a
one-line change to the `swagger-ui.version` property in `pom.xml`.

## Maven

Applications must add the following dependency to their pom.xml,

```
<dependency>
  <groupId>org.wrensecurity.commons</groupId>
  <artifactId>openapi-war-overlay</artifactId>
  <version>${project.version}</version>
  <type>war</type>
</dependency>
```

The WAR overlay is applied via an overlay entry in the `maven-war-plugin`,

```
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-war-plugin</artifactId>
      <executions>
        <execution>
          <id>war</id>
          <phase>package</phase>
          <goals>
            <goal>war</goal>
          </goals>
          <configuration>
            <classifier>servlet</classifier>
            <overlays>
              <overlay>
                <groupId>org.wrensecurity.commons</groupId>
                <artifactId>openapi-war-overlay</artifactId>
              </overlay>
            </overlays>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

## web.xml

For embedded Jetty servers, the following lines in `web.xml` will make the
assets available via HTTP,

```
<servlet>
    <servlet-name>OpenAPIAssetsServlet</servlet-name>
    <servlet-class>org.eclipse.jetty.ee11.servlet.ResourceServlet</servlet-class>
    <init-param>
        <param-name>pathInfoOnly</param-name>
        <param-value>false</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>OpenAPIAssetsServlet</servlet-name>
    <url-pattern>/openapi/*</url-pattern>
</servlet-mapping>
```

Keep in mind that the `ResourceServlet` has to be made available via jetty-web.xml:

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "https://eclipse.dev/jetty/configure_12.dtd">
<Configure class="org.eclipse.jetty.ee11.webapp.WebAppContext">
    <!-- Expose Jetty servlet classes to be usable in web.xml (e.g. ResourceServlet). -->
    <Get name="hiddenClassMatcher">
        <Call name="exclude">
            <Arg>org.eclipse.jetty.ee11.servlet.</Arg>
        </Call>
    </Get>
</Configure>
```
