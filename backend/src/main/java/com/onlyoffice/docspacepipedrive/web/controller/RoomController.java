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

import com.onlyoffice.docspacepipedrive.client.pipedrive.PipedriveClient;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDeal;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDealFollower;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Room;
import com.onlyoffice.docspacepipedrive.events.deal.AddRoomToPipedriveDealEvent;
import com.onlyoffice.docspacepipedrive.exceptions.RequestAccessToRoomException;
import com.onlyoffice.docspacepipedrive.exceptions.RoomNotFoundException;
import com.onlyoffice.docspacepipedrive.manager.DocspaceActionManager;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.RoomService;
import com.onlyoffice.docspacepipedrive.web.dto.room.RoomRequest;
import com.onlyoffice.docspacepipedrive.web.dto.room.RoomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    private final RoomService roomService;
    private final DocspaceAccountService docspaceAccountService;
    private final PipedriveClient pipedriveClient;
    private final DocspaceActionManager docspaceActionManager;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping("/{dealId}")
    public ResponseEntity<RoomResponse> findByDealId(
            @AuthenticationPrincipal OAuth2PipedriveUser currentUser,
            @PathVariable Long dealId
    ) {
        PipedriveDeal pipedriveDeal = pipedriveClient.getDeal(dealId);

        Room room;
        try {
            room = roomService.findByClientIdAndDealId(currentUser.getClientId(), dealId);
        } catch (RoomNotFoundException e) {
            room = Room.builder()
                    .dealId(dealId)
                    .roomId(null)
                    .build();
        }

        return ResponseEntity.ok(
                new RoomResponse(
                        Objects.toString(room.getRoomId(), null),
                        MessageFormat.format(
                                "{0} - Pipedrive ({1})",
                                pipedriveDeal.getTitle(),
                                currentUser.getAttribute("company_name")
                        )
                )
        );
    }

    @PostMapping("/{dealId}")
    public ResponseEntity<RoomResponse> save(@AuthenticationPrincipal OAuth2PipedriveUser currentUser,
                                               @PathVariable Long dealId,
                                               @Valid @RequestBody RoomRequest request) {
        PipedriveDeal pipedriveDeal = pipedriveClient.getDeal(dealId);

        Room relatedRoom;
        try {
            relatedRoom = roomService.findByClientIdAndDealId(currentUser.getClientId(), dealId);
        } catch (RoomNotFoundException e) {
            relatedRoom = roomService.create(
                    currentUser.getClientId(),
                    Room.builder()
                            .roomId(request.getRoomId())
                            .dealId(pipedriveDeal.getId())
                            .build()
            );

            eventPublisher.publishEvent(
                    new AddRoomToPipedriveDealEvent(this,
                            currentUser.getClientId(),
                            pipedriveDeal,
                            relatedRoom.getRoomId()
                    )
            );
        }

        return ResponseEntity.ok(
                new RoomResponse(
                        relatedRoom.getRoomId().toString(),
                        null
                )
        );
    }

    @PostMapping("/{dealId}/request-access")
    public ResponseEntity<Void> requestAccess(@AuthenticationPrincipal OAuth2PipedriveUser currentUser,
                                              @PathVariable Long dealId) {
        Room room = roomService.findByClientIdAndDealId(currentUser.getClientId(), dealId);

        List<PipedriveDealFollower> dealFollowers = pipedriveClient.getDealFollowers(dealId);

        boolean currentUserIsDealFollower = dealFollowers.stream()
                .anyMatch(dealFollower -> dealFollower.getUserId().equals(currentUser.getUserId()));

        if (!currentUserIsDealFollower) {
            throw new RequestAccessToRoomException(currentUser.getClientId(), currentUser.getUserId());
        }

        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(
                currentUser.getClientId(),
                currentUser.getUserId()
        );

        docspaceActionManager.inviteListDocspaceAccountsToRoom(
                room.getRoomId(),
                Collections.singletonList(docspaceAccount)
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{dealId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal OAuth2PipedriveUser currentUser,
                                       @PathVariable Long dealId) {
        roomService.deleteByClientIdAndDealId(currentUser.getClientId(), dealId);

        return ResponseEntity.noContent().build();
    }
}
