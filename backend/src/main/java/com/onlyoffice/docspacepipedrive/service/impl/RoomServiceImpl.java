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

package com.onlyoffice.docspacepipedrive.service.impl;

import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.entity.Room;
import com.onlyoffice.docspacepipedrive.exceptions.RoomNotFoundException;
import com.onlyoffice.docspacepipedrive.repository.RoomRepository;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final ClientService clientService;
    private final RoomRepository roomRepository;

    @Override
    public Room findByClientIdAndDealId(final Long clientId, final Long dealId) {
        return roomRepository.findByClientIdAndDealId(clientId, dealId)
                .orElseThrow(() -> new RoomNotFoundException(clientId, dealId));
    }

    @Override
    public Room create(final Long clientId, final Room room) {
        Client client = clientService.findById(clientId);

        room.setClient(client);

        return roomRepository.save(room);
    }

    @Override
    public Room update(final Long clientId, final Room room) {
        Room existedRoom = findByClientIdAndDealId(clientId, room.getDealId());

        existedRoom.setRoomId(room.getRoomId());

        return roomRepository.save(room);
    }

    @Override
    public void deleteAllByClientId(final Long clientId) {
        roomRepository.deleteAllByClientId(clientId);
    }

    @Override
    @Transactional
    public void deleteByClientIdAndDealId(final Long clientId, final Long dealId) {
        roomRepository.deleteByClientIdAndDealId(clientId, dealId);
    }
}
