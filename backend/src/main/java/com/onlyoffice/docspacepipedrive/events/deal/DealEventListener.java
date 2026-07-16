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

package com.onlyoffice.docspacepipedrive.events.deal;

import com.onlyoffice.docspacepipedrive.client.pipedrive.PipedriveClient;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDeal;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDealFollower;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDealFollowerEvent;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveUserSettings;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Room;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.exceptions.RoomNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIdNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIsNotPresentInResponse;
import com.onlyoffice.docspacepipedrive.exceptions.UserNotFoundException;
import com.onlyoffice.docspacepipedrive.manager.DocspaceActionManager;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.RoomService;
import com.onlyoffice.docspacepipedrive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class DealEventListener {
    private final DocspaceActionManager docspaceActionManager;
    private final PipedriveClient pipedriveClient;
    private final UserService userService;
    private final RoomService roomService;
    private final DocspaceAccountService docspaceAccountService;

    @EventListener
    public void listen(final AddRoomToPipedriveDealEvent event) {
        PipedriveDeal pipedriveDeal = event.getPipedriveDeal();
        Long roomId = event.getRoomId();

        PipedriveUserSettings pipedriveUserSettings = pipedriveClient.getUserSettings();

        int visibleToEveryone = PipedriveDeal.VisibleTo.EVERYONE.integer();
        if (pipedriveUserSettings.getAdvancedPermissions()) {
            visibleToEveryone = PipedriveDeal.VisibleTo.EVERYONE_ADVANCED_PERMISSIONS.integer();
        }

        // Invite shared group to room if deal visible for everyone
        if (pipedriveDeal.getVisibleTo().equals(visibleToEveryone)) {
            try {
                docspaceActionManager.inviteSharedGroupToRoom(roomId);
            } catch (SharedGroupIdNotFoundException | SharedGroupIsNotPresentInResponse e) {
                log.warn(e.getMessage());
                log.warn(MessageFormat.format(
                        "Try re-init Shared Group for Client ID({0})",
                        event.getClientId().toString()
                ));

                docspaceActionManager.initSharedGroup();

                log.warn(MessageFormat.format(
                        "Shared Group successfully initialized for Client ID({0})",
                        event.getClientId().toString()
                ));

                docspaceActionManager.inviteSharedGroupToRoom(roomId);
            }
        }

        // Invite all deal followers to room
        List<PipedriveDealFollower> dealFollowers = pipedriveClient.getDealFollowers(pipedriveDeal.getId());

        List<DocspaceAccount> docspaceAccounts = docspaceAccountService.findAllByClientIdAndUserIds(
                event.getClientId(),
                dealFollowers.stream()
                        .map(PipedriveDealFollower::getUserId)
                        .toList()
        );

        docspaceActionManager.inviteListDocspaceAccountsToRoom(roomId, docspaceAccounts);
    }

    @EventListener
    public void listen(final AddVisibleEveryoneForPipedriveDealEvent event) {
        PipedriveDeal pipedriveDeal = event.getPipedriveDeal();

        Room room;
        try {
            room = roomService.findByClientIdAndDealId(event.getClientId(), pipedriveDeal.getId());
        } catch (RoomNotFoundException e) {
            // Ignore it if there is no DocSpace room for the Pipedrive deal
            return;
        }

        try {
            docspaceActionManager.inviteSharedGroupToRoom(room.getRoomId());
        } catch (SharedGroupIdNotFoundException | SharedGroupIsNotPresentInResponse e) {
            log.warn(e.getMessage());
            log.warn(MessageFormat.format(
                    "Try re-init Shared Group for Client ID({0})",
                    event.getClientId().toString()
            ));

            docspaceActionManager.initSharedGroup();

            log.warn(MessageFormat.format(
                    "Shared Group successfully initialized for Client ID({0})",
                    event.getClientId().toString()
            ));

            docspaceActionManager.inviteSharedGroupToRoom(room.getRoomId());
        }
    }

    @EventListener
    public void listen(final RemoveVisibleEveryoneForPipedriveDealEvent event) {
        PipedriveDeal pipedriveDeal = event.getPipedriveDeal();

        Room room;
        try {
            room = roomService.findByClientIdAndDealId(event.getClientId(), pipedriveDeal.getId());
        } catch (RoomNotFoundException e) {
            // Ignore it if there is no DocSpace room for the Pipedrive deal
            return;
        }

        docspaceActionManager.removeSharedGroupFromRoom(event.getClientId(), room.getRoomId());
    }

    @EventListener
    public void listen(final AddFollowersToPipedriveDealEvent event) {
        PipedriveDeal pipedriveDeal = event.getPipedriveDeal();

        Room room;
        try {
            room = roomService.findByClientIdAndDealId(event.getClientId(), pipedriveDeal.getId());
        } catch (RoomNotFoundException e) {
            // Ignore it if there is no DocSpace room for the Pipedrive deal
            return;
        }

        List<PipedriveDealFollowerEvent> dealFollowerEvents = pipedriveClient.getDealFollowersFlow(
                pipedriveDeal.getId()
        );

        List<Long> userIdsAddedFollowers = findUserIdsInDealFollowersEvents(
                dealFollowerEvents,
                "added",
                pipedriveDeal.getUpdateTime()
        );

        List<User> addedFollowers = new ArrayList<>();
        for (Long userId : userIdsAddedFollowers) {
            try {
                addedFollowers.add(userService.findByClientIdAndUserId(event.getClientId(), userId));
            } catch (UserNotFoundException e) {
                // Do nothing if the UserNotFoundException
            }
        }

        List<DocspaceAccount> docspaceAccounts = addedFollowers.stream()
                .filter(user -> user.getDocspaceAccount() != null)
                .map(user -> user.getDocspaceAccount())
                .toList();

        docspaceActionManager.inviteListDocspaceAccountsToRoom(room.getRoomId(), docspaceAccounts);
    }

    @EventListener
    public void listen(final RemoveFollowersFromPipedriveDealEvent event) {
        PipedriveDeal pipedriveDeal = event.getPipedriveDeal();

        Room room;
        try {
            room = roomService.findByClientIdAndDealId(event.getClientId(), pipedriveDeal.getId());
        } catch (RoomNotFoundException e) {
            // Ignore it if there is no DocSpace room for the Pipedrive deal
            return;
        }

        List<PipedriveDealFollowerEvent> dealFollowerEvents = pipedriveClient.getDealFollowersFlow(
                pipedriveDeal.getId()
        );

        List<Long> userIdsRemovedFollowers = findUserIdsInDealFollowersEvents(
                dealFollowerEvents,
                "removed",
                pipedriveDeal.getUpdateTime()
        );

        List<User> removedFollowers = new ArrayList<>();
        for (Long userId : userIdsRemovedFollowers) {
            try {
                removedFollowers.add(userService.findByClientIdAndUserId(event.getClientId(), userId));
            } catch (UserNotFoundException e) {
                // Do nothing if UserNotFoundException
            }
        }

        List<DocspaceAccount> docspaceAccounts = removedFollowers.stream()
                .filter(user -> user.getDocspaceAccount() != null)
                .map(user -> user.getDocspaceAccount())
                .toList();

        docspaceActionManager.removeListDocspaceAccountsFromRoom(room.getRoomId(), docspaceAccounts);
    }

    private List<Long> findUserIdsInDealFollowersEvents(final List<PipedriveDealFollowerEvent> dealFollowerEvents,
                                                        final String action, final String time) {
        return dealFollowerEvents.stream()
                .filter(followerEvent -> {
                    return followerEvent.getData().getAction().equals(action)
                            && followerEvent.getData().getLogTime().equals(time);
                })
                .map(follower -> follower.getData().getFollowerUserId())
                .toList();
    }
}
