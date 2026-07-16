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
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveUser;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyNotFoundException;
import com.onlyoffice.docspacepipedrive.web.dto.docspaceaccount.DocspaceAccountRequest;
import com.onlyoffice.docspacepipedrive.web.dto.docspaceaccount.DocspaceAccountResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
public class UserControllerTest extends AbstractControllerTest {
    @Test
    public void whenGetUser_thenReturnOk() throws Exception {
        String response = mockMvc.perform(get("/api/v1/user")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> actualResponse = objectMapper.readValue(response, Map.class);

        Map<String, Object> expectedResponse = objectMapper.readValue(
                objectMapper.writeValueAsString(
                    Map.of(
                    "id", 10000,
                    "name", "Test User 10000",
                    "isAdmin", true,
                    "language", new PipedriveUser.Language("en", "US"),
                    "docspaceAccount", new DocspaceAccountResponse(
                            testDocspaceAccount.getEmail(),
                            testDocspaceAccount.getPasswordHash()
                        )
                    )
                ), Map.class
        );

        for (Map.Entry<String, Object> entry : expectedResponse.entrySet()) {
            assertTrue(actualResponse.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), actualResponse.get(entry.getKey()));
        }
    }

    @Test
    public void whenPutAlreadyExistsDocspaceAccount_thenReturnForbidden() throws Exception {
        DocspaceAccountRequest docspaceAccountRequest = new DocspaceAccountRequest(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1",
                "docspace.user1@onlyoffice.com",
                "password_hash"
        );

        mockMvc.perform(put("/api/v1/user/docspace-account")
                        .queryParam("system", "false")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(docspaceAccountRequest))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    public void whenPutDocspaceAccountWithoutSettings_thenReturnForbidden() throws Exception {
        settingsService.clear(testClient.getId());

        DocspaceAccountRequest docspaceAccountRequest = new DocspaceAccountRequest(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
                "docspace.user2@onlyoffice.com",
                "password_hash"
        );

        String response = mockMvc.perform(put("/api/v1/user/docspace-account")
                        .queryParam("system", "false")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserNotSalesAdmin)
                        )
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(docspaceAccountRequest))
                )
                .andExpect(status().isServiceUnavailable())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        assertEquals(
                responseMap.get("message"),
                new DocspaceApiKeyNotFoundException(testClient.getId()).getMessage()
        );
    }

    @Test
    public void whenDeleteDocspaceAccount_thenReturnOk() throws Exception {
        String response = mockMvc.perform(delete("/api/v1/user/docspace-account")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserSalesAdmin)
                        )
                )
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    public void whenDeleteNotExistsDocspaceAccount_thenReturnOk() throws Exception {
        String response = mockMvc.perform(delete("/api/v1/user/docspace-account")
                        .header("Authorization",
                                getAuthorizationHeaderForUser(testUserNotSalesAdmin)
                        )
                )
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
