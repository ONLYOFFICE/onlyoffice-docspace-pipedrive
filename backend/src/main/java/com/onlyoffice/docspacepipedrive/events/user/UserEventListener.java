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

package com.onlyoffice.docspacepipedrive.events.user;

import com.onlyoffice.docspacepipedrive.entity.Webhook;
import com.onlyoffice.docspacepipedrive.manager.DocspaceActionManager;
import com.onlyoffice.docspacepipedrive.manager.PipedriveActionManager;
import com.onlyoffice.docspacepipedrive.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {
    private final DocspaceActionManager docspaceActionManager;
    private final PipedriveActionManager pipedriveActionManager;
    private final WebhookService webhookService;

    @EventListener
    public void listen(final DocspaceLoginUserEvent event) {
        docspaceActionManager.inviteDocspaceAccountToSharedGroup(event.getDocspaceAccount().getUuid());
    }

    @EventListener
    public void listen(final DocspaceLogoutUserEvent event) {
        try {
            docspaceActionManager.removeDocspaceAccountFromSharedGroup(
                    event.getDocspaceAccount().getUuid()
            );
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @EventListener
    public void listen(final UserOwnerWebhooksIsLostEvent event) {
        List<Webhook> webhooks = webhookService.findAllByClientIdAndUserId(event.getClientId(), event.getUserId());

        pipedriveActionManager.deleteWebhooks(webhooks);

    }
}
