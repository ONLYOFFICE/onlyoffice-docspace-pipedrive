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

package com.onlyoffice.docspacepipedrive.entity;

import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.exceptions.SharedGroupIdNotFoundException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Optional;
import java.util.UUID;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "settings")
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String url;
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "api_key_value")),
            @AttributeOverride(name = "ownerId", column = @Column(name = "api_key_owner_id")),
            @AttributeOverride(name = "valid", column = @Column(name = "api_key_valid"))
    })
    private ApiKey apiKey;
    private UUID sharedGroupId;
    @OneToOne
    @JoinColumn(name = "client_id")
    @ToString.Exclude
    private Client client;

    public UUID getSharedGroupId() {
        return Optional.ofNullable(sharedGroupId).orElseThrow(
                () -> new SharedGroupIdNotFoundException(this.client.getId())
        );
    }

    public Boolean existSharedGroupId() {
        return sharedGroupId != null;
    }
}
