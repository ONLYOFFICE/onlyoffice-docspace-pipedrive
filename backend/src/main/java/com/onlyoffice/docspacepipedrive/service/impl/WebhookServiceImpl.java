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

import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.Webhook;
import com.onlyoffice.docspacepipedrive.exceptions.UserNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.WebhookNotFoundException;
import com.onlyoffice.docspacepipedrive.repository.UserRepository;
import com.onlyoffice.docspacepipedrive.repository.WebhookRepository;
import com.onlyoffice.docspacepipedrive.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {
    private final UserRepository userRepository;
    private final WebhookRepository webhookRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean existsByClientIdAndName(final Long clientId, final String name) {
        return webhookRepository.existsByUser_Client_IdAndName(clientId, name);
    }

    @Override
    public List<Webhook> findAllByClientId(final Long clientId) {
        return webhookRepository.findAllByUser_Client_Id(clientId);
    }

    @Override
    public List<Webhook> findAllByClientIdAndUserId(final Long clientId, final Long userId) {
        return webhookRepository.findAllByUser_Client_IdAndUser_UserId(clientId, userId);
    }

    @Override
    public Webhook findById(final UUID id) {
        return webhookRepository.findById(id)
                .orElseThrow(() -> new WebhookNotFoundException(id));
    }

    @Override
    public Webhook save(final Long clientId, final Long userId, final Webhook webhook) {
        User user = userRepository.findByClientIdAndUserId(clientId, userId)
                .orElseThrow(() -> new UserNotFoundException(clientId, userId));

        webhook.setUser(user);
        webhook.setPassword(passwordEncoder.encode(webhook.getPassword()));
        return webhookRepository.save(webhook);
    }

    @Override
    public void deleteById(final UUID id) {
        webhookRepository.deleteById(id);
    }
}
