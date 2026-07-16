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

package com.onlyoffice.docspacepipedrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.standalone.JsonFileMappingsSource;
import com.onlyoffice.docspacepipedrive.configuration.TestSecurityConfiguration;
import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.repository.ClientRepository;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.RoomService;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import com.onlyoffice.docspacepipedrive.service.UserService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


@SpringBootTest(classes = TestSecurityConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractControllerTest {
    protected static Client testClient;
    protected static User testUserSalesAdmin;
    protected static User testUserNotSalesAdmin;
    protected static DocspaceAccount testDocspaceAccount;

    @Container
    protected static final PostgreSQLContainer POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2")).withReuse(true);

    @Container
    protected static final RedisContainer REDIS_CONTAINER =
            new RedisContainer(DockerImageName.parse("redis:7.0.12"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @RegisterExtension
    protected static final WireMockExtension WIREMOCK_PIPEDRIVE_SERVER = WireMockExtension.newInstance()
            .options(
                    wireMockConfig().dynamicPort()
                            .mappingSource(
                                    new JsonFileMappingsSource(
                                            wireMockConfig().filesRoot()
                                                    .child("mappings")
                                                    .child("pipedrive"),
                                            null
                                    )
                            )
            )
            .build();

    @RegisterExtension
    protected static final WireMockExtension WIREMOCK_DOCSPACE_SERVER = WireMockExtension.newInstance()
            .options(
                    wireMockConfig().dynamicPort()
                            .mappingSource(
                                    new JsonFileMappingsSource(
                                            wireMockConfig().filesRoot()
                                                    .child("mappings")
                                                    .child("docspace"),
                                            null
                                    )
                            )
            )
            .build();

    @Autowired
    protected SettingsService settingsService;
    @Autowired
    protected ClientService clientService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected DocspaceAccountService docspaceAccountService;
    @Autowired
    protected RoomService roomService;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected JwtEncoder jwtEncoder;
    @Autowired
    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.jwt.client-name-attribute}")
    private String clientNameAttribute;
    @Value("${spring.security.jwt.user-name-attribute}")
    private String userNameAttribute;

    @DynamicPropertySource
    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());

        registry.add(
                "spring.security.oauth2.client.provider.pipedrive.user-info-uri",
                () -> WIREMOCK_PIPEDRIVE_SERVER.baseUrl() + "/v1/users/me"
        );
        registry.add("pipedrive.base-api-url", WIREMOCK_PIPEDRIVE_SERVER::baseUrl);
    }

    @BeforeEach
    public void setup() {
        testClient = clientService.create(
                Client.builder()
                        .id(10000L)
                        .url(WIREMOCK_PIPEDRIVE_SERVER.baseUrl())
                        .build()
        );

        testUserSalesAdmin = userService.put(
                testClient.getId(),
                TestUtils.createUser(10000L, testClient.getId())
        );

        testUserNotSalesAdmin = userService.put(
                testClient.getId(),
                TestUtils.createUser(10001L, testClient.getId())
        );

        settingsService.put(testClient.getId(), Settings.builder()
                .url(WIREMOCK_DOCSPACE_SERVER.baseUrl())
                .apiKey(ApiKey.builder()
                        .value("sk-api-key-for-test")
                        .ownerId(UUID.randomUUID())
                        .valid(true)
                        .build())
                .sharedGroupId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1"))
                .build());

        testDocspaceAccount = docspaceAccountService.save(
                testUserSalesAdmin.getClient().getId(),
                testUserSalesAdmin.getUserId(),
                TestUtils.createDocspaceAccount(1L)
        );

        testClient = clientService.update(testClient);
    }

    @AfterEach
    public void unset() {
        clientRepository.deleteAll();
    }

    protected String getAuthorizationHeaderForUser(final User user) {
        return MessageFormat.format(
                "Bearer {0}",
                getJwtTokenForUser(user).getTokenValue()
        );
    }

    protected Jwt getJwtTokenForUser(final User user) {
        Instant now = Instant.now();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claims(stringObjectMap -> {
                    stringObjectMap.put(clientNameAttribute, user.getClient().getId());
                    stringObjectMap.put(userNameAttribute, user.getUserId());
                })
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims));
    }

}
