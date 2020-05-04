# PlanQK Atlas Developer Guide

This document provides an index to all development guidelines and background information of the PlanQK Atlas.

- [ADR](../adr) - list of [architectural decision records](https://adr.github.io) showing which design decisions were taken during development of Winery

## Quick Develop

1. Clone the repository `git clone https://github.com/PlanQK/QC-Atlas.git`.
2. Build the repository `mvn package -DskipTests` (skiping the tests for a faster build), Java 8 required.
3. Continue your IDE setup:
    - [IntelliJ Ultimate](config/IntelliJ%20IDEA/)
    - [Eclipse](config/Eclipse/)
4. Start Postgres Database:
    - Clone the repository `git clone https://github.com/PlanQK/planqk-docker.git`.
    - Startup in this directory via the [Compose Dev File](https://github.com/PlanQK/planqk-docker/blob/master/docker-compose.dev.yml):
        ```
        $ docker-compose -f docker-compose.dev.yml pull
        $ docker-compose -f docker-compose.dev.yml up
        ``` 
5. Start the application (via the runconfig that you configured in step 3) 

## Main API Enpoints
API-Root: /atlas

Swagger-Documentation: /atlas/swagger-ui

Hal-Browser: /atlas/browser 

## License

Copyright (c) 2013-2018 Contributors to the Eclipse Foundation

See the NOTICE file(s) distributed with this work for additional
information regarding copyright ownership.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
which is available at https://www.apache.org/licenses/LICENSE-2.0.

SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
