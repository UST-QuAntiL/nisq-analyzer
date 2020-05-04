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

package org.planqk.atlas.web.dtos.entities;

import java.util.List;

import org.planqk.atlas.core.model.Provider;
import org.planqk.atlas.core.model.Qpu;
import org.planqk.atlas.core.model.Sdk;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.RepresentationModel;

/**
 * Data transfer object for the model class {@link Qpu}.
 */
@ToString(callSuper = true, includeFieldNames = true)
public class QpuDto extends RepresentationModel<ProviderDto> {

    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private int numberOfQubits;

    @Getter
    @Setter
    private float t1;

    @Getter
    @Setter
    private float maxGateTime;

    public static final class Converter {

        public static QpuDto convert(final Qpu object) {
            QpuDto dto = new QpuDto();
            dto.setId(object.getId());
            dto.setName(object.getName());
            dto.setNumberOfQubits(object.getQubitCount());
            dto.setT1(object.getT1());
            dto.setMaxGateTime(object.getMaxGateTime());
            return dto;
        }

        public static Qpu convert(final QpuDto object, final Provider provider, final List<Sdk> supportedSdks) {
            Qpu qpu = new Qpu();
            qpu.setName(object.getName());
            qpu.setQubitCount(object.getNumberOfQubits());
            qpu.setT1(object.getT1());
            qpu.setMaxGateTime(object.getMaxGateTime());
            qpu.setProvider(provider);
            qpu.setSupportedSdks(supportedSdks);
            return qpu;
        }
    }
}
