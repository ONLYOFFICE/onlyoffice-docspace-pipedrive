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

package com.onlyoffice.docspacepipedrive.configuration;

import com.onlyoffice.docspacepipedrive.manager.PipedriveActionManager;
import com.onlyoffice.docspacepipedrive.security.AuthenticationEntryPointImpl;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2AuthenticationSuccessHandler;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2LoginCancelFilter;
import com.onlyoffice.docspacepipedrive.security.provider.ClientRegistrationAuthenticationProvider;
import com.onlyoffice.docspacepipedrive.security.provider.JwtAuthenticationProvider;
import com.onlyoffice.docspacepipedrive.security.provider.WebhookAuthenticationProvider;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.crypto.spec.SecretKeySpec;


@Configuration
@EnableWebSecurity
@EnableRedisHttpSession
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {
    private final AuthenticationEntryPointImpl authenticationEntryPoint;

    @Value("${app.frontend-url}")
    private String frontendUrl;
    @Value("${db.encryption.password}")
    private String encryptPassword;
    @Value("${db.encryption.salt}")
    private String encryptSalt;
    @Value("${spring.security.oauth2.client.registration.pipedrive.client-secret}")
    private String jwtSecretKey;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final BasicAuthenticationFilter clientRegistrationAuthenticationFilter,
                                                   final BearerTokenAuthenticationFilter jwtAuthenticationFilter,
                                                   final BasicAuthenticationFilter webhookAuthenticationFilter,
                                                   final OAuth2LoginCancelFilter oAuth2LoginCancelFilter,
                                                   final OAuth2AuthenticationSuccessHandler auth2AuthenticationSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/v1/health").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .requestMatchers(HttpMethod.DELETE, "/login/oauth2/code/{registrationId}").authenticated()
                            .anyRequest().permitAll();
                })
                .oauth2Login(oauth -> {
                    oauth.successHandler(auth2AuthenticationSuccessHandler);
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(httpSecurityCorsConfigurer -> {
                    httpSecurityCorsConfigurer
                            .configurationSource(corsConfigurationSource());
                })
                .sessionManagement(httpSecuritySessionManagementConfigurer -> {
                        httpSecuritySessionManagementConfigurer
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
                        httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterAfter(new ForwardedHeaderFilter(), WebAsyncManagerIntegrationFilter.class)
                .addFilterBefore(oAuth2LoginCancelFilter, OAuth2LoginAuthenticationFilter.class)
                .addFilterBefore(clientRegistrationAuthenticationFilter, OAuth2LoginAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, OAuth2LoginAuthenticationFilter.class)
                .addFilterAfter(webhookAuthenticationFilter, OAuth2LoginAuthenticationFilter.class);

        SecurityFilterChain securityFilterChain = http.build();

        for (Filter filter : securityFilterChain.getFilters()) {
            if (filter instanceof OAuth2LoginAuthenticationFilter) {
                ((OAuth2LoginAuthenticationFilter) filter).setRequiresAuthenticationRequestMatcher(
                        new AntPathRequestMatcher("/login/oauth2/code/*", "GET")
                );
            }
        }

        return securityFilterChain;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOrigin(frontendUrl);
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    BasicAuthenticationFilter clientRegistrationAuthenticationFilter(final HttpSecurity http,
                                                                     final ClientRegistrationAuthenticationProvider clientRegistrationAuthenticationProvider) throws Exception {
        var authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);

        authManagerBuilder.authenticationProvider(clientRegistrationAuthenticationProvider);

        AuthenticationManager authenticationManager = authManagerBuilder.build();
        return new BasicAuthenticationFilter(authenticationManager, authenticationEntryPoint) {
            @Override
            protected boolean shouldNotFilter(final HttpServletRequest request) {
                return new NegatedRequestMatcher(
                        new AntPathRequestMatcher("/login/oauth2/code/{registrationId}", "DELETE")
                ).matches(request);
            }
        };
    }

    @Bean
    BearerTokenAuthenticationFilter jwtAuthenticationFilter(final HttpSecurity http,
                                                            final JwtAuthenticationProvider jwtAuthenticationProvider) throws Exception {
        var authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder.authenticationProvider(jwtAuthenticationProvider);

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        BearerTokenAuthenticationFilter jwtAuthenticationFilter =
                new BearerTokenAuthenticationFilter(authenticationManager) {
            @Override
            protected boolean shouldNotFilter(final HttpServletRequest request) {
                return new AntPathRequestMatcher("/api/v1/webhook/**").matches(request);
            }
        };

        jwtAuthenticationFilter.setAuthenticationEntryPoint(authenticationEntryPoint);

        return jwtAuthenticationFilter;
    }

    @Bean
    BasicAuthenticationFilter webhookAuthenticationFilter(final HttpSecurity http,
                                                          final WebhookAuthenticationProvider webhookAuthenticationProvider) throws Exception {
        var authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);

        authManagerBuilder.authenticationProvider(webhookAuthenticationProvider);

        AuthenticationManager authenticationManager = authManagerBuilder.build();

        return new BasicAuthenticationFilter(authenticationManager, authenticationEntryPoint) {
            @Override
            protected boolean shouldNotFilter(final HttpServletRequest request) {
                return new NegatedRequestMatcher(new AntPathRequestMatcher("/api/v1/webhook/**")).matches(request);
            }
        };
    }

    @Bean
    public OAuth2LoginCancelFilter oAuth2LoginCancelFilter(
            final ClientRegistrationRepository clientRegistrationRepository) {
        return new OAuth2LoginCancelFilter(clientRegistrationRepository);
    }

    @Bean
    public OAuth2AuthenticationSuccessHandler auth2AuthenticationSuccessHandler(
            final ClientRegistrationRepository clientRegistrationRepository,
            final PipedriveActionManager pipedriveActionManager, final SettingsService settingsService) {
        return new OAuth2AuthenticationSuccessHandler(clientRegistrationRepository, pipedriveActionManager,
                settingsService);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(getJwtSecretKey()).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public TextEncryptor textEncryptor() {
        return Encryptors.text(encryptPassword, encryptSalt);
    }

    private SecretKeySpec getJwtSecretKey() {
        byte[] bytes = jwtSecretKey.getBytes();
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
