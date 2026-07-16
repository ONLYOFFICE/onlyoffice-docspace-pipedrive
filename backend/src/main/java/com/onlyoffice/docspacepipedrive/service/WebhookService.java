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

package com.onlyoffice.docspacepipedrive.service;

import com.onlyoffice.docspacepipedrive.entity.Webhook;

import java.util.List;
import java.util.UUID;


public interface WebhookService {
    boolean existsByClientIdAndName(Long clientId, String name);
    List<Webhook> findAllByClientId(Long clientId);
    List<Webhook> findAllByClientIdAndUserId(Long clientId, Long userId);
    Webhook findById(UUID id);
    Webhook save(Long clientId, Long userId, Webhook webhook);
    void deleteById(UUID id);
}
