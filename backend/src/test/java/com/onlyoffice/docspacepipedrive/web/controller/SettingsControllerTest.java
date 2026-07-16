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

package com.onlyoffice.docspacepipedrive.web.controller;

import com.onlyoffice.docspacepipedrive.AbstractControllerTest;
import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIdNotFoundException;
import com.onlyoffice.docspacepipedrive.web.dto.settings.SettingsRequest;
import com.onlyoffice.docspacepipedrive.web.dto.settings.SettingsResponse;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
public class SettingsControllerTest extends AbstractControllerTest {

    @Test
    public void whenGetSetting_thenReturnOk() throws Exception {
        String actualResponse = mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expectedResponse = objectMapper.writeValueAsString(
                new SettingsResponse(
                        WIREMOCK_DOCSPACE_SERVER.baseUrl(),
                        "sk-***test",
                        true,
                        false
                )
        );

        JsonAssert.assertJsonEquals(expectedResponse, actualResponse);
    }

    @Test
    public void whenGetEmptySetting_thenReturnOk() throws Exception {
        settingsService.clear(testClient.getId());

        String actualResponse = mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expectedResponse = objectMapper.writeValueAsString(
            new SettingsResponse(
                null,
                null,
                false,
                false
            )
        );

        JsonAssert.assertJsonEquals(expectedResponse, actualResponse);
    }

    @Test
    public void whenPutSetting_thenReturnOk() throws Exception {
        SettingsRequest settingsRequest = new SettingsRequest(
                WIREMOCK_DOCSPACE_SERVER.baseUrl(),
                "sk-api-key-test"
        );

        String actualResponse = mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(settingsRequest))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String expectedResponse = objectMapper.writeValueAsString(
                new SettingsResponse(
                        WIREMOCK_DOCSPACE_SERVER.baseUrl(),
                        "sk-***test",
                        true,
                        true
                )
        );

        JsonAssert.assertJsonEquals(expectedResponse, actualResponse);
    }

    @Test
    public void whenPostSetting_notSalesAdmin_thenReturnForbidden() throws Exception {
        SettingsRequest settingsRequest = new SettingsRequest(
                WIREMOCK_DOCSPACE_SERVER.baseUrl(),
                "sk-api-key-test"
        );

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserNotSalesAdmin)
                        )
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(settingsRequest))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenDeleteSetting_thenReturnOk() throws Exception {
        mockMvc.perform(delete("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                )
                .andExpect(status().isNoContent());

        Client client = clientService.findById(testClient.getId());

        assertNull(client.getSettings().getUrl());
        assertThrows(SharedGroupIdNotFoundException.class, () -> client.getSettings().getSharedGroupId());
    }

    @Test
    public void whenDeleteSetting_notSalesAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/settings")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserNotSalesAdmin)
                        )
                )
                .andExpect(status().isForbidden());
    }
}
