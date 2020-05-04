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

import org.planqk.atlas.core.model.Tag;

import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

public class TagDto extends RepresentationModel<TagDto> {

    @Getter
    @Setter
    private String key;

    @Getter
    @Setter
    private String value;

    @Getter
    @Setter
    private Long id;

    public static final class Converter {

        public static TagDto convert(final Tag object) {
            final TagDto dto = new TagDto();
            dto.setId(object.getId());
            dto.setKey(object.getKey());
            dto.setValue(object.getValue());
            return dto;
        }

        public static Tag convert(final TagDto object) {
            final Tag tag = new Tag();
            tag.setId(object.getId());
            tag.setKey(object.getKey());
            tag.setValue(object.getValue());
            return tag;
        }
    }
}
