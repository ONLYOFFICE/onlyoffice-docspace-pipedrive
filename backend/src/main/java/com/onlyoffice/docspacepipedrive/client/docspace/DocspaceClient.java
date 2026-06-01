/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

package com.onlyoffice.docspacepipedrive.client.docspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceApiKey;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceCSPSettings;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceFile;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceFileUploadSession;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceGroup;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceMembers;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspacePayload;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoom;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomInvitationRequest;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomType;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceUser;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface DocspaceClient {
    String authenticate(String docspaceUrl, String userName, String passwordHash);
    void logout(String docspaceUrl, String token);
    DocspaceCSPSettings getCSPSettings();
    DocspaceCSPSettings updateCSPSettings(List<String> domains);
    List<DocspaceApiKey> getApiKeys();
    DocspaceUser getUser();
    DocspaceUser getUser(String email);
    DocspaceUser getUser(UUID id);
    List<DocspaceUser> findUsers(Integer employeeType);
    DocspaceRoom createRoom(String title, DocspaceRoomType roomType, List<String> tags);
    DocspaceMembers shareRoom(Long roomId, DocspaceRoomInvitationRequest docspaceRoomInvitationRequest);
    DocspaceGroup createGroup(String name, UUID owner, List<UUID> members);
    DocspaceGroup updateGroup(UUID groupId, String groupName, UUID groupManager, List<UUID> membersToAdd,
                              List<UUID> membersToRemove);
    Mono<DocspaceFile> getFile(Long fileId, String docspaceUrl, String token);
    Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(Long fileId, String docspaceUrl, String token);
    Mono<DocspacePayload<DocspaceFileUploadSession>> createFileUploadSession(Long folderId, String fileName,
                                                                             long fileSize, String docspaceUrl,
                                                                             String token);
    Mono<DocspacePayload<JsonNode>> uploadFileChunk(String sessionId, byte[] chunk, String docspaceUrl, String token);
}
