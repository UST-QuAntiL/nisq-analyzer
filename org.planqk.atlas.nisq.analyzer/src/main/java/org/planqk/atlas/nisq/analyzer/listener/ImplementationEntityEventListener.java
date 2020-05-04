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

package org.planqk.atlas.nisq.analyzer.listener;

import org.planqk.atlas.core.events.EntityCreatedEvent;
import org.planqk.atlas.core.model.Implementation;
import org.planqk.atlas.nisq.analyzer.knowledge.prolog.PrologFactUpdater;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ImplementationEntityEventListener {

    private final PrologFactUpdater prologFactUpdater;

    public ImplementationEntityEventListener(PrologFactUpdater prologFactUpdater) {
        this.prologFactUpdater = prologFactUpdater;
    }

    @Async
    @EventListener
    public void onImplementationCreatedEvent(EntityCreatedEvent<Implementation> event) {
        Implementation impl = event.getEntity();

        prologFactUpdater.handleImplementationInsertion(impl.getId(), impl.getSdk().getName(), impl.getImplementedAlgorithm().getId(), impl.getSelectionRule(), impl.getWidthRule(), impl.getDepthRule());
    }

//    @Async
//    @EventListener
//    public void onImplementationUpdatedEvent(... <Implementation> event) {
//        Implementation impl = event.getEntity();
//
//        //PrologFactUpdater.handleImplementationUpdate(impl.getId(), impl.getSdk().getName(),
//                //impl.getImplementedAlgorithm().getId(), impl.getSelectionRule());
//    }
//
//    @Async
//    @EventListener
//    public void onImplementationRemovedEvent(... <Implementation> event) {
//        Implementation impl = event.getEntity();
//
//        //PrologFactUpdater.handleImplementationDeletion(impl.getId());
//    }
}
