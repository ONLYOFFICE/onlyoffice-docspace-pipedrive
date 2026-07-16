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

import com.onlyoffice.docspacepipedrive.client.pipedrive.PipedriveClient;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveWebhook;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.Webhook;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.security.util.RandomPasswordGenerator;
import com.onlyoffice.docspacepipedrive.security.util.SecurityUtils;
import com.onlyoffice.docspacepipedrive.service.UserService;
import com.onlyoffice.docspacepipedrive.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PipedriveActionManager {
    private static final int WEBHOOK_PASSWORD_LENGTH = 32;
    private final PipedriveClient pipedriveClient;
    private final WebhookService webhookService;
    private final UserService userService;

    @Value("${app.base-url}")
    private String baseUrl;

    public boolean isWebhooksInstalled(final Long clientId) {
        return webhookService.existsByClientIdAndName(
                clientId,
                "deal.updated"
        ) && webhookService.existsByClientIdAndName(
                clientId,
                "user.updated"
        );
    }

    @Transactional
    public void initWebhooks() {
        initWebhook("deal", "updated");
        initWebhook("user", "updated");
    }

    @Transactional
    public void deleteWebhooks(final List<Webhook> webhooks) {
        for (Webhook webhook : webhooks) {
            deleteWebhook(webhook);
        }
    }

    private void initWebhook(final String eventObject, final String eventAction) {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();

        if (webhookService.existsByClientIdAndName(currentUser.getClientId(), eventObject + "." + eventAction)) {
            return;
        }

        Webhook webhook = Webhook.builder()
                .name(eventObject + "." + eventAction)
                .password(RandomPasswordGenerator.generatePassword(WEBHOOK_PASSWORD_LENGTH))
                .build();

        Webhook savedWebhook = webhookService.save(currentUser.getClientId(), currentUser.getUserId(), webhook);

        PipedriveWebhook pipedriveWebhook = PipedriveWebhook.builder()
                .subscriptionUrl(baseUrl + "/api/v1/webhook/" + eventObject)
                .eventAction(eventAction)
                .eventObject(eventObject)
                .httpAuthUser(savedWebhook.getId().toString())
                .httpAuthPassword(webhook.getPassword())
                .version("1.0")
                .build();

        pipedriveWebhook = pipedriveClient.createWebhook(pipedriveWebhook);

        savedWebhook.setWebhookId(pipedriveWebhook.getId());
        webhookService.save(currentUser.getClientId(), currentUser.getUserId(), savedWebhook);
    }

    private void deleteWebhook(final Webhook webhook) {
        User webhookOwner = webhook.getUser();

        try {
            SecurityUtils.runAs(new SecurityUtils.RunAsWork<Void>() {
                public Void doWork() {
                    pipedriveClient.deleteWebhook(webhook.getWebhookId());
                    return null;
                }
            }, webhookOwner.getClient().getId(), webhookOwner.getUserId());
        } catch (Exception e) {
            log.warn("Error deleting webhook in Pipedrive (WebhookID: {})", webhook.getWebhookId(), e);
        } finally {
            webhookService.deleteById(webhook.getId());
        }
    }
}
