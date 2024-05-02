/*******************************************************************************
 * Copyright (c) 2022 University of Stuttgart
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.planqk.nisq.analyzer.core.connector.qiskit.IbmqQpuQueue;
import org.planqk.nisq.analyzer.core.model.Provider;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.web.dtos.entities.ProviderListDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class QProvService {

    final private static Logger LOG = LoggerFactory.getLogger(QProvService.class);

    // API Endpoints
    private String providerAPIEnpoint;

    private List<Qpu> requiredQpuList;

    public QProvService(
        @Value("${org.planqk.nisq.analyzer.qprov.hostname}") String hostname,
        @Value("${org.planqk.nisq.analyzer.qprov.port}") int port
    ) {
        this.providerAPIEnpoint = String.format("http://%s:%d/qprov/providers", hostname, port);
    }

    public List<Provider> getProviders() {

        // Query the QProv API for providers
        RestTemplate restTemplate = new RestTemplate();

        try {
            ProviderListDto result = restTemplate.getForObject(providerAPIEnpoint, ProviderListDto.class);
            if (result != null) {
                return ProviderListDto.Converter.convert(result);
            } else {
                return new ArrayList<>();
            }
        } catch (RestClientException e) {
            LOG.error("Error while connecting to QPROV: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Qpu> getQPUs(Provider provider) {

        RestTemplate restTemplate = new RestTemplate();

        try {
            QpuListDto qpuListDto =
                restTemplate.getForObject(URI.create(String.format(providerAPIEnpoint + "/%s/qpus", provider.getId())), QpuListDto.class);
            if (qpuListDto != null) {
                return requiredQpuList = qpuListDto.getQpuDtoList().stream().map(dto -> QpuDto.Converter.convert(dto, provider.getName())).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        } catch (RestClientException e) {
            LOG.error("Error while connecting to QPROV: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Optional<Qpu> getQpuByName(String name, String provider) {
        if (requiredQpuList != null) {
            Optional<Qpu> givenQpu = requiredQpuList.stream().filter(p -> p.getName().equals(name)).findFirst();
            if (givenQpu.isPresent()) {
                return givenQpu;
            }
        }
        Optional<Provider> prov = getProviders().stream().filter(p -> p.getName().equals(provider)).findFirst();
        return prov.flatMap(value -> getQPUs(value).stream().filter(q -> q.getName().equalsIgnoreCase(name)).findFirst());
    }

    public Integer getQueueSizeOfQpu(String qpuName, String provider) {
        if (provider.equalsIgnoreCase("ionq")) {
            Optional<Qpu> optionalQpu = getQpuByName(qpuName, provider);
            if (optionalQpu.isPresent()) {
                Qpu qpu = optionalQpu.get();
                return qpu.getQueueSize();
            }
        } else {
            URI ibmqQueueSizeUrl = URI.create(String.format("https://api.quantum-computing.ibm.com/api/Backends/%s/queue/status?", qpuName));
            LOG.debug("Requesting IBMQ for queue size");
            RestTemplate restTemplate = new RestTemplate();

            // fake user agent, as IBMQ blocks Java/1.8
            HttpHeaders headers = new HttpHeaders();
            headers.set("user-agent", "python-requests/2.27.1");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<IbmqQpuQueue> response =
                    restTemplate.exchange(ibmqQueueSizeUrl, HttpMethod.GET, entity, IbmqQpuQueue.class);

                IbmqQpuQueue ibmqQpuQueue = response.getBody();

                if (ibmqQpuQueue != null) {
                    return ibmqQpuQueue.getLengthQueue();
                } else {
                    return 100;
                }
            } catch (RestClientException e) {
                return 100;
            }
        }
        return 100;
    }
}
