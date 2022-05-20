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
                // In case the result isn't empty, extract it.
                providerList = ProviderListDto.Converter.convert(result);
            } else {
                //In case QProv does not return providers, return an empty list.
                providerList = new ArrayList<>();
            }
            return providerList;
        } catch (RestClientException e) {
            return new ArrayList<>();
        }
    }

    public List<Qpu> getQPUs(Provider provider) {
        RestTemplate restTemplate = new RestTemplate();
        // ToDo: Implement proper QPU List class
        QpuListDto qpuListDto =
                restTemplate.getForObject(String.format(this.baseAPIEndpoint + "/providers/%s/qpus", provider.getId()), QpuListDto.class);

        return qpuListDto.getQpuDtoList().stream().map(dto -> QpuDto.Converter.convert(dto, provider.getName())).collect(Collectors.toList());
    }

    public Optional<Qpu> getQpuByName(String name, String provider) {
        Optional<Provider> prov = getProviders().stream().filter(p -> p.getName().equals(provider)).findFirst();
        return prov.flatMap(value -> getQPUs(value).stream().filter(q -> q.getName().equals(name)).findFirst());
    }
}
