/*******************************************************************************
 * Copyright (c) 2020 University of Stuttgart
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.planqk.nisq.analyzer.core.qprov;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.Constants;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ProviderListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class QProvService {
    final private static Logger LOG = LoggerFactory.getLogger(QProvService.class);

    // API Endpoints
    private String baseAPIEndpoint;

    public QProvService(
            @Value("${org.planqk.nisq.analyzer.qprov.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.qprov.port}") int port
    ) {
        this.baseAPIEndpoint = String.format("http://%s:%d/qprov/", hostname, port);
    }

    public List<Provider> getProviders() {

        // Query the QProv API for providers
        RestTemplate restTemplate = new RestTemplate();
        try {
            ProviderListDto result = restTemplate.getForObject(this.baseAPIEndpoint + "providers", ProviderListDto.class);

            List<Provider> providerList;
            if (result != null && result.getEmbedded() != null) {
                //Since QProv does not currently support the Cirq Google Provider, it needs to be manually added to the list
                providerList = ProviderListDto.Converter.convert(result);
            } else {
                //In case QProv does not return providers, just return the locally supported cirq-google provider
                providerList = new ArrayList<>();
            }
            Provider googleProv = new Provider();
            googleProv.setName(Constants.GOOGLE);
            providerList.add(googleProv);
            return providerList;
        } catch (RestClientException e) {
            return new ArrayList<>();
        }
    }

    public List<Qpu> getQPUs(Provider provider) {
        // Since QProv does not currently support Google QPUs, they are added Manually with filler informations
        if (provider.getName().equalsIgnoreCase(Constants.GOOGLE)) {
            // All supported Google QPUs
            List<String> names = new ArrayList<>(4);
            names.add("sycamore");
            names.add("sycamore23");
            names.add("bristlecone");
            names.add("foxtail");
            List<Qpu> qpus = new ArrayList<>(4);
            for (String name: names) {
                // Adding real information
                Qpu qpu = new Qpu();
                qpu.setProvider(Constants.GOOGLE);
                // Currently, while code can be transpiled for these real QPUs, the execution can only be simulated due to missing access tokens.
                qpu.setSimulator(true);
                qpu.setName(name);
                // Add dummy information
                qpu.setAvgMultiQubitGateError(-1);
                qpu.setAvgMultiQubitGateTime(-1);
                qpu.setAvgReadoutError(-1);
                qpu.setMaxGateTime(-1);
                qpu.setAvgReadoutError(-1);
                qpu.setAvgSingleQubitGateError(-1);
                qpu.setAvgSingleQubitGateTime(-1);
                qpu.setQubitCount(-1);
                qpu.setQueueSize(-1);
                qpu.setT1(-1);
                qpu.setT2(-1);
                qpus.add(qpu);
            }
            return qpus;
        } else {
            RestTemplate restTemplate = new RestTemplate();
            // ToDo: Implement proper QPU List class
            QpuListDto qpuListDto =
                    restTemplate.getForObject(String.format(this.baseAPIEndpoint + "/providers/%s/qpus", provider.getId()), QpuListDto.class);

            return qpuListDto.getQpuDtoList().stream().map(dto -> QpuDto.Converter.convert(dto, provider.getName())).collect(Collectors.toList());
        }
    }

    public Optional<Qpu> getQpuByName(String name, String provider) {
        Optional<Provider> prov = getProviders().stream().filter(p -> p.getName().equals(provider)).findFirst();
        return prov.flatMap(value -> getQPUs(value).stream().filter(q -> q.getName().equals(name)).findFirst());
    }
}
