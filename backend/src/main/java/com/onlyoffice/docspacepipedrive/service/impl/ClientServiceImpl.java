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
import com.onlyoffice.docspacepipedrive.exceptions.ClientNotFoundException;
import com.onlyoffice.docspacepipedrive.repository.ClientRepository;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;

    @Override
    public Client findById(final Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));

        return client;
    }

    @Override
    public boolean existById(final Long id) {
        return clientRepository.existsById(id);
    }

    @Override
    public Client create(final Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Client update(final Client client) {
        Client existedClient = findById(client.getId());

        if (StringUtils.hasText(client.getUrl())) {
            existedClient.setUrl(client.getUrl());
        }

        if (StringUtils.hasText(client.getCompanyName())) {
            existedClient.setCompanyName(client.getCompanyName());
        }

        return clientRepository.save(existedClient);
    }

    @Override
    public void delete(final Long id) {
        clientRepository.deleteById(id);
    }
}
