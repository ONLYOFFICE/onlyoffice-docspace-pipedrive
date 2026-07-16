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

package com.onlyoffice.docspacepipedrive.events.settings;

import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.entity.Webhook;
import com.onlyoffice.docspacepipedrive.manager.DocspaceActionManager;
import com.onlyoffice.docspacepipedrive.manager.PipedriveActionManager;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.RoomService;
import com.onlyoffice.docspacepipedrive.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class SettingsEventListener {
    private final ClientService clientService;
    private final RoomService roomService;
    private final DocspaceAccountService docspaceAccountService;
    private final WebhookService webhookService;
    private final DocspaceActionManager docspaceActionManager;
    private final PipedriveActionManager pipedriveActionManager;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @EventListener
    public void listen(final SettingsUpdateEvent event) {
        Client client = clientService.findById(event.getClientId());

        docspaceActionManager.addDomainsToCSPSettings(Arrays.asList(client.getUrl(), frontendUrl));
        docspaceActionManager.initSharedGroup();
        pipedriveActionManager.initWebhooks();
    }

    @EventListener
    @Transactional
    public void listen(final SettingsDeleteEvent event) {
        List<Webhook> webhooks = webhookService.findAllByClientId(event.getClientId());
        pipedriveActionManager.deleteWebhooks(webhooks);

        roomService.deleteAllByClientId(event.getClientId());
        docspaceAccountService.deleteAllByClientId(event.getClientId());
    }
}
