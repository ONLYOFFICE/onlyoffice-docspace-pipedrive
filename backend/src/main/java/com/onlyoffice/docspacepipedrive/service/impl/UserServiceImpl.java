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
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.exceptions.UserNotFoundException;
import com.onlyoffice.docspacepipedrive.repository.UserRepository;
import com.onlyoffice.docspacepipedrive.security.RedisAuthenticationRepository;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final RedisAuthenticationRepository redisAuthenticationRepository;

    @Override
    public User findById(final Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public User findByClientIdAndUserId(final Long clientId, final Long userId) {
        return userRepository.findByClientIdAndUserId(clientId, userId)
                .orElseThrow(() -> new UserNotFoundException(clientId, userId));
    }

    @Override
    public List<User> findAllByClientId(final Long clientId) {
        return userRepository.findAllByClientId(clientId);
    }

    @Override
    public User put(final Long clientId, final User user) {
        try {
            User existedUser = findByClientIdAndUserId(clientId, user.getUserId());

            if (user.getAccessToken() != null) {
                existedUser.setAccessToken(user.getAccessToken());
            }

            if (user.getRefreshToken() != null) {
                existedUser.setRefreshToken(user.getRefreshToken());
            }

            return userRepository.save(existedUser);
        } catch (UserNotFoundException e) {
            Client client = clientService.findById(clientId);

            user.setClient(client);

            return userRepository.save(user);
        }
    }

    @Override
    public void deleteByUserIdAndClientId(final Long userId, final Long clientId) {
        userRepository.delete(findByClientIdAndUserId(clientId, userId));
        redisAuthenticationRepository.deleteAuthentication(clientId + ":" + userId);
    }
}
