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

package org.planqk.atlas.core.services;

import java.util.List;
import java.util.Optional;

import org.planqk.atlas.core.events.EntityCreatedEvent;
import org.planqk.atlas.core.model.Qpu;
import org.planqk.atlas.core.repository.QpuRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QpuServiceImpl implements QpuService {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final QpuRepository repository;

    @Override
    public Qpu save(Qpu qpu) {
        Qpu savedQpu = repository.save(qpu);

        applicationEventPublisher.publishEvent(new EntityCreatedEvent<>(savedQpu));

        return savedQpu;
    }

    @Override
    public Page<Qpu> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Qpu> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Qpu> findById(Long qpuId) {
        return repository.findById(qpuId);
    }
}
