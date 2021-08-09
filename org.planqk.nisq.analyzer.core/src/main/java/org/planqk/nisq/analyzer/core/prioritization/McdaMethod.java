/*******************************************************************************
 * Copyright (c) 2021 University of Stuttgart
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

package org.planqk.nisq.analyzer.core.prioritization;

/**
 * Interface for the implementation of a MCDA method.
 */
public interface McdaMethod {

    /**
     * Returns the unique name of the implemented MCDA method
     *
     * @return the name of the MCDA method
     */
    String getName();

    /**
     * Returns the textual description of the implemented MCDA method
     *
     * @return the textual description of the MCDA method
     */
    String getDescription();
}
