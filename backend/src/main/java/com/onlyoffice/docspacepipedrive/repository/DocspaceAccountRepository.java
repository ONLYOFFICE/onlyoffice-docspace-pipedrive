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

package com.onlyoffice.docspacepipedrive.repository;

import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface DocspaceAccountRepository extends JpaRepository<DocspaceAccount, Long> {
    Optional<DocspaceAccount> findByUser_Client_IdAndUser_UserId(Long clientId, Long userId);

    @Modifying
    @Query(value = "DELETE FROM docspace_accounts WHERE user_id IN "
            + "(SELECT id FROM users WHERE client_id = :clientId)",
            nativeQuery = true)
    void deleteAllByUser_Client_Id(Long clientId);

    @Modifying
    @Query(value = "DELETE FROM docspace_accounts WHERE user_id IN "
            + "(SELECT id FROM users WHERE client_id = :clientId AND user_id = :userId)",
            nativeQuery = true)
    void deleteByUser_Client_IdAndUser_UserId(Long clientId, Long userId);
    List<DocspaceAccount> findAllByUser_Client_IdAndUser_UserIdIn(Long clientId, List<Long> userIds);
}
