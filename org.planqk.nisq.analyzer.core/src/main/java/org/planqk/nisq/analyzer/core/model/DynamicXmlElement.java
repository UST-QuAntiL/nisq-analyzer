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

package org.planqk.nisq.analyzer.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;

/**
 * Wrapper to generate custom XML elements not defined by the XMCDA library.
 * <p>
 * See: https://stackoverflow.com/questions/47587631/how-to-add-dynamic-attribute-to-dynamic-element-in-jaxb
 */
public class DynamicXmlElement {

    @XmlAnyAttribute
    private final Map<QName, String> attributes;

    @XmlAnyElement
    private final List<Object> elements;

    public DynamicXmlElement() {
        attributes = new LinkedHashMap<>();
        elements = new ArrayList<>();
    }

    public void addAttribute(QName name, String value) {
        attributes.put(name, value);
    }

    public void addElement(Object element) {
        elements.add(element);
    }
}
