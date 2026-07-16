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

package com.onlyoffice.docspacepipedrive.security.util;

import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static OAuth2PipedriveUser getCurrentUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if (authentication != null) {
            if (authentication.getPrincipal() instanceof OAuth2PipedriveUser) {
                return (OAuth2PipedriveUser) authentication.getPrincipal();
            }
        }

        return null;
    }

    public static <R> R runAs(final RunAsWork<R> runAsWork, final Long clientId, final Long userId) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication currentAuthentication = securityContext.getAuthentication();

        final R result;
        try {
            securityContext.setAuthentication(new OAuth2AuthenticationToken(
                    new OAuth2PipedriveUser(
                            Map.of(
                                    "id", userId,
                                    "company_id", clientId
                            )
                    ),
                    List.of(),
                    "pipedrive"
            ));

            result = runAsWork.doWork();
            return result;
        } catch (Throwable exception) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException("Error during run as.", exception);
            }
        } finally {
            securityContext.setAuthentication(currentAuthentication);
        }
    }

    public interface RunAsWork<Result> {
        Result doWork() throws Exception;
    }

}
