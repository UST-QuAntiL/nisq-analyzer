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
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.util.Arrays;
import org.planqk.nisq.analyzer.core.model.Qpu;
import org.planqk.nisq.analyzer.core.model.Sdk;
import org.planqk.nisq.analyzer.core.repository.SdkRepository;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuDto;
import org.planqk.nisq.analyzer.core.web.dtos.entities.QpuListDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class QProvService {

    // API Endpoints
    private String baseAPIEndpoint;

    public QProvService(
            @Value("${org.planqk.nisq.analyzer.qprov.hostname}") String hostname,
            @Value("${org.planqk.nisq.analyzer.qprov.port}") int port
    ) {
        this.baseAPIEndpoint = String.format("http://%s:%d/qprov/", hostname, port);
    }

    public List<Provider> getProviders() {

        RestTemplate restTemplate = new RestTemplate();

        // Query the QProv API for providers
        ProviderList result = restTemplate.getForObject(this.baseAPIEndpoint + "providers", ProviderList.class);

        if (result != null) {
            return result.getProviders();
        } else {
            return new ArrayList<>();
        }

    }

    public List<Qpu> getQPUs(Provider provider) {

        RestTemplate restTemplate = new RestTemplate();

        // ToDo: Implement proper QPU List class
        QpuListDto qpuListDto = restTemplate.getForObject(String.format(this.baseAPIEndpoint + "/providers/%s/qpus", provider.getId()), QpuListDto.class);


        // ToDo: create a list of supported SDKs (dummy)

        return qpuListDto.getQpuDtoList().stream().map(dto -> QpuDto.Converter.convert(dto, new ArrayList<>())).collect(Collectors.toList());
    }

}
