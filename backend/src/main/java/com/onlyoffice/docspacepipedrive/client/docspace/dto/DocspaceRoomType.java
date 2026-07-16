/**
 *
 * (c) Copyright Ascensio System SIA 2026
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
 *
 */

package com.onlyoffice.docspacepipedrive.client.docspace.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum DocspaceRoomType {
    FILLING_FORMS_ROOM(1),
    EDITING_ROOM(2),
    CUSTOM_ROOM(5),
    PUBLIC_ROOM(6),
    VIRTUAL_DATA_ROOM(8);

    private static final Map<Integer, DocspaceRoomType> BY_ID = new HashMap<>();

    private final int id;

    static {
        for (DocspaceRoomType e: values()) {
            BY_ID.put(e.getId(), e);
        }
    }

    @JsonCreator
    public static DocspaceRoomType valueOfId(final Integer code) {
        return BY_ID.get(code);
    }

    @JsonValue
    int getId() {
        return this.id;
    }
}
