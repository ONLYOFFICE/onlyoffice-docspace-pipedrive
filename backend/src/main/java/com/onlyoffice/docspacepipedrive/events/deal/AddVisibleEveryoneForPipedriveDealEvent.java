/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDeal;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public class AddVisibleEveryoneForPipedriveDealEvent extends ApplicationEvent {
    private final PipedriveDeal pipedriveDeal;

    public AddVisibleEveryoneForPipedriveDealEvent(final Object source, final PipedriveDeal pipedriveDeal) {
        super(source);
        this.pipedriveDeal = pipedriveDeal;
    }
}
