<!--
  The contents of this file are subject to the terms of the Common Development and
  Distribution License (the License). You may not use this file except in compliance with the
  License.

  You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
  specific language governing permission and limitations under the License.

  When distributing Covered Software, include this CDDL Header Notice in each file and include
  the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
  Header, with the fields enclosed by brackets [] replaced by your own identifying
  information: "Portions copyright [year] [name of copyright owner]".

  Copyright 2026 Wren Security
-->

# Wren Security Commons

[![License](https://img.shields.io/badge/license-CDDL-blue.svg)](https://github.com/WrenSecurity/wrensec-commons/blob/main/LICENSE)
[![Gitter](https://img.shields.io/matrix/wrensecurity_lobby%3Agitter.im?server_fqdn=matrix.org)](https://matrix.to/#/#WrenSecurity_Lobby:gitter.im)

Wren Security Commons is a set of reusable libraries and frameworks shared across [Wren Security](https://wrensecurity.org) projects.
It provides common functionality for identity management, access management, HTTP/REST services, and more.

Wren Security Commons is one of the projects in the Wren Security Suite, a community initiative that adopted open‐source projects formerly developed by ForgeRock, which has its own roots in Sun Microsystems' products.

The modules of Wren Security Commons include:

| Module | Description |
|--------|-------------|
| **commons-bom** | Bill of Materials for dependency management |
| **audit** | Audit logging framework with multiple handlers (CSV, Syslog, JMS, JDBC, JSON, Elasticsearch, Splunk) |
| **auth-filters** | Authentication and authorization filters (JASPI runtime, authn-filter, authz-filter) |
| **bloomfilter** | Thread-safe Bloom Filter implementations (scalable and rolling) |
| **guice** | Shaded Google Guice dependency (core, servlet, test) |
| **http-framework** | Uniform client/server HTTP API with adapters (Servlet, Apache HTTP, Grizzly, OAuth2) |
| **i18n** | Internationalization framework with Maven plugin and logging integration |
| **json-crypto** | JSON encryption and decryption (core library and CLI) |
| **json-ref** | JSON Reference resolution |
| **json-schema** | JSON Schema validation (core library and CLI) |
| **json-web-token** | JSON Web Token (JWT) library |
| **rest** | Common REST server framework (json-resource, HTTP bindings, API descriptor) |
| **self-service** | Self-service processes framework (password reset, user self-service, custom stages) |
| **security** | Common instantiation and handling of keystores |
| **util** | Shared utility and test utility classes |

## Contributions

[![Contributing Guide](https://img.shields.io/badge/Contributions-guide-green.svg?style=flat)](https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines)
[![Contributors](https://img.shields.io/github/contributors/WrenSecurity/wrensec-commons)](https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines)
[![Pull Requests](https://img.shields.io/github/issues-pr/WrenSecurity/wrensec-commons)](https://github.com/WrenSecurity/wrensec-docs/wiki/Contributor-Guidelines)
[![Last commit](https://img.shields.io/github/last-commit/WrenSecurity/wrensec-commons.svg)](https://github.com/WrenSecurity/wrensec-commons/commits/main)

## Building the Project

**Prepare your Environment**

The following software is needed to build the project:

| Software | Required Version |
|----------|------------------|
| OpenJDK  | 17 and above     |
| Git      | 2.0 and above    |
| Maven    | 3.0 and above    |

**Build the source code**

All project dependencies are hosted in JFrog repository and managed by Maven, so to build the project simply execute Maven *install* goal.

```
$ cd $GIT_REPOSITORIES/wrensec-commons
$ mvn clean install
```

## Acknowledgments

Wren Security Commons is standing on the shoulders of giants and is a continuation of a prior work:

- OpenAM by Sun Microsystems
- OpenIDM, OpenDJ, OpenIG by ForgeRock AS

We'd like to thank them for supporting the idea of open-source software.

## Disclaimer

Please note that the acknowledged parties are not affiliated with this project.
Their trade names, product names and trademarks should not be used to refer to the Wren Security products, as it might be considered an unfair commercial practice.

Wren Security is open source and always will be.
