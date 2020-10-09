# NISQ-Analyzer

[![Build Status](https://api.travis-ci.com/UST-QuAntiL/nisq-analyzer.svg?branch=master)](https://travis-ci.com/UST-QuAntiL/nisq-analyzer)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Build

1. Run `mvn package -DskipTests` inside the root folder.
2. When completed, the built product can be found in `org.planqk.nisq.analyzer.core/target`.

## Setup

* For running the QuAntiL environment with all its components use the docker-compose of [quantil-docker](https://github.com/UST-QuAntiL/quantil-docker).  

* Clone repository:
```
git clone https://github.com/UST-QuAntiL/nisq-analyzer.git   
git clone git@github.com:UST-QuAntiL/nisq-analyzer.git
```

* Start NISQ Analyzer and PostgreSQL containers:
```
docker-compose pull
docker-compose up
```

* Additionally, the [Qiskit Service](https://github.com/UST-QuAntiL/qiskit-service/tree/SummerSoC2020) can be started by adapting the command:
```
docker-compose -f docker-compose.yml -f ../qiskit-service/docker-compose.yml pull
docker-compose -f docker-compose.yml -f ../qiskit-service/docker-compose.yml up
```

Now the NISQ Analyzer is available on http://localhost:8081/.
If you also started the Qiskit Service, it is available on http://localhost:5000/.
	
## Running on Tomcat

Build the project and deploy the WAR file located at `org.planqk.nisq.analyzer.core/target` to Tomcat.

Make sure you have an accessibly Postgres database and configure the application correspondingly.

Prerequisites:

- [SWI Prolog](https://www.swi-prolog.org/) is installed on the machine where the Tomcat runs and the Path is configured correspondingly

## Haftungsausschluss

Dies ist ein Forschungsprototyp.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden, ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE.
You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.
