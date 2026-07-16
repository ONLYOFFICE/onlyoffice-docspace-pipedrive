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
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.repository.SettingsRepository;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {
    private final ClientService clientService;
    private final SettingsRepository settingsRepository;

    @Override
    public Settings findByClientId(final Long clientId) {
        return settingsRepository.findByClientId(clientId)
                .orElse(new Settings());
    }

    @Override
    public Settings put(final Long clientId, final Settings settings) {
        Settings existedSetting = settingsRepository.findByClientId(clientId)
                .orElse(null);

        if (Objects.nonNull(existedSetting)) {
            if (StringUtils.hasText(settings.getUrl())) {
                existedSetting.setUrl(settings.getUrl());
            }

            if (Objects.nonNull(settings.getApiKey())) {
                existedSetting.setApiKey(settings.getApiKey());
            }

            return settingsRepository.save(existedSetting);
        }

        Client client = clientService.findById(clientId);
        settings.setClient(client);

        return settingsRepository.save(settings);
    }

    @Override
    public Settings saveSharedGroup(final Long clientId, final UUID groupId) {
        Settings existedSetting = findByClientId(clientId);

        existedSetting.setSharedGroupId(groupId);

        return settingsRepository.save(existedSetting);
    }

    @Override
    public Settings setApiKeyValid(final Long clientId, final boolean valid) {
        Settings existedSetting = findByClientId(clientId);

        existedSetting.getApiKey().setValid(valid);

        return settingsRepository.save(existedSetting);
    }

    @Override
    public void clear(final Long clientId) {
        Settings existedSetting = findByClientId(clientId);

        existedSetting.setUrl(null);
        existedSetting.setSharedGroupId(null);
        existedSetting.setApiKey(null);

        settingsRepository.save(existedSetting);
    }
}
