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

package com.onlyoffice.docspacepipedrive.manager;

import com.onlyoffice.docspacepipedrive.client.docspace.DocspaceClient;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceAccess;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceCSPSettings;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceGroup;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceMembers;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomInvitation;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomInvitationRequest;
import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceWebClientResponseException;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIdNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIsNotPresentInResponse;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.security.util.SecurityUtils;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import com.onlyoffice.docspacepipedrive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class DocspaceActionManager {
    private final DocspaceClient docspaceClient;
    private final SettingsService settingsService;
    private final ClientService clientService;
    private final UserService userService;

    public void initSharedGroup() {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();
        Client client = clientService.findById(currentUser.getClientId());
        Settings settings = settingsService.findByClientId(currentUser.getClientId());
        ApiKey apiKey = settings.getApiKey();

        if (Objects.isNull(apiKey)) {
            throw new DocspaceApiKeyNotFoundException(currentUser.getClientId());
        }

        List<User> users = userService.findAllByClientId(currentUser.getClientId());
        List<UUID> members = users.stream()
                .filter(user -> user.getDocspaceAccount() != null)
                .map(user -> user.getDocspaceAccount().getUuid())
                .toList();

        if (!settings.existSharedGroupId()) {
            DocspaceGroup docspaceGroup = docspaceClient.createGroup(
                    MessageFormat.format("Pipedrive Users ({0})", client.getCompanyName()),
                    apiKey.getOwnerId(),
                    members
            );

            settingsService.saveSharedGroup(
                    currentUser.getClientId(),
                    docspaceGroup.getId()
            );
        } else {
            try {
                docspaceClient.updateGroup(
                        settings.getSharedGroupId(),
                        null,
                        apiKey.getOwnerId(),
                        members,
                        null
                );
            } catch (WebClientResponseException e) {
                if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    settingsService.saveSharedGroup(
                            currentUser.getClientId(),
                            null
                    );

                    initSharedGroup();
                    return;
                }

                throw e;
            }
        }
    }

    public void inviteDocspaceAccountToSharedGroup(final UUID docspaceAccountId) {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();
        Settings settings = settingsService.findByClientId(currentUser.getClientId());

        try {
            docspaceClient.updateGroup(
                    settings.getSharedGroupId(),
                    null,
                    null,
                    Collections.singletonList(docspaceAccountId),
                    null
            );
        } catch (SharedGroupIdNotFoundException e) {
            initSharedGroup();
        } catch (DocspaceWebClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                settingsService.saveSharedGroup(
                        currentUser.getClientId(),
                        null
                );

                initSharedGroup();
                return;
            }

            throw e;
        }
    }

    public void removeDocspaceAccountFromSharedGroup(final UUID docspaceAccountId) {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();
        Settings settings = settingsService.findByClientId(currentUser.getClientId());

        docspaceClient.updateGroup(
                settings.getSharedGroupId(),
                null,
                null,
                null,
                Collections.singletonList(docspaceAccountId)
        );
    }

    public void inviteSharedGroupToRoom(final Long roomId) {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();
        Settings settings = settingsService.findByClientId(currentUser.getClientId());
        UUID sharedGroupId = settings.getSharedGroupId();

        DocspaceRoomInvitation docspaceRoomInvitation =
                new DocspaceRoomInvitation(
                        sharedGroupId,
                        DocspaceAccess.COLLABORATOR
                );

        DocspaceRoomInvitationRequest docspaceRoomInvitationRequest = DocspaceRoomInvitationRequest.builder()
                .invitations(Collections.singletonList(docspaceRoomInvitation))
                .message("Invitation message")
                .notify(true)
                .build();

        DocspaceMembers docspaceMembers = docspaceClient.shareRoom(roomId, docspaceRoomInvitationRequest);
        boolean sharedGroupIsPresentInResponse = docspaceMembers.getMembers().stream()
                .filter(docspaceMember -> docspaceMember.getSharedTo().getId().equals(sharedGroupId))
                .findFirst()
                .isPresent();

        if (!sharedGroupIsPresentInResponse) {
            throw new SharedGroupIsNotPresentInResponse(sharedGroupId, roomId);
        }
    }

    public void removeSharedGroupFromRoom(final Long clientId, final Long roomId) {
        Settings settings = settingsService.findByClientId(clientId);
        UUID sharedGroupId = settings.getSharedGroupId();

        DocspaceRoomInvitation docspaceRoomInvitation =
                new DocspaceRoomInvitation(
                        sharedGroupId,
                        DocspaceAccess.NONE
                );

        DocspaceRoomInvitationRequest docspaceRoomInvitationRequest = DocspaceRoomInvitationRequest.builder()
                .invitations(Collections.singletonList(docspaceRoomInvitation))
                .notify(false)
                .build();

        docspaceClient.shareRoom(roomId, docspaceRoomInvitationRequest);
    }

    public void inviteListDocspaceAccountsToRoom(final Long roomId, final List<DocspaceAccount> docspaceAccounts) {
        List<DocspaceRoomInvitation> invitations = docspaceAccounts.stream()
                .map(docspaceAccount -> {
                    return new DocspaceRoomInvitation(
                            docspaceAccount.getUuid(),
                            DocspaceAccess.COLLABORATOR
                    );
                })
                .toList();

        if (invitations.size() > 0) {
            DocspaceRoomInvitationRequest docspaceRoomInvitationRequest = DocspaceRoomInvitationRequest.builder()
                    .invitations(invitations)
                    .message("Invitation message")
                    .notify(true)
                    .build();

            docspaceClient.shareRoom(roomId, docspaceRoomInvitationRequest);
        }
    }

    public void removeListDocspaceAccountsFromRoom(final Long roomId, final List<DocspaceAccount> docspaceAccounts) {
        List<DocspaceRoomInvitation> invitations = docspaceAccounts.stream()
                .map(docspaceAccount -> new DocspaceRoomInvitation(
                        docspaceAccount.getUuid(),
                        DocspaceAccess.NONE
                ))
                .toList();

        if (invitations.size() > 0) {
            DocspaceRoomInvitationRequest docspaceRoomInvitationRequest = DocspaceRoomInvitationRequest.builder()
                    .invitations(invitations)
                    .notify(false)
                    .build();

            docspaceClient.shareRoom(roomId, docspaceRoomInvitationRequest);
        }
    }

    public DocspaceCSPSettings addDomainsToCSPSettings(final List<String> domains) {
        DocspaceCSPSettings docspaceCSPSettings = docspaceClient.getCSPSettings();

        List<String> allowedDomains = docspaceCSPSettings.getDomains();

        List<String> notAllowedDomains = domains.stream()
                .filter(domain -> !allowedDomains.contains(domain))
                .toList();

        if (!notAllowedDomains.isEmpty()) {
            allowedDomains.addAll(notAllowedDomains);

            return docspaceClient.updateCSPSettings(allowedDomains);
        }

        return docspaceCSPSettings;
    }

}
