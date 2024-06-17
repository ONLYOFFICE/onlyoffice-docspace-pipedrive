package com.onlyoffice.docspacepipedrive.client.pipedrive;

import com.onlyoffice.docspacepipedrive.client.pipedrive.response.PipedriveDeal;
import com.onlyoffice.docspacepipedrive.client.pipedrive.response.PipedriveResponse;
import com.onlyoffice.docspacepipedrive.client.pipedrive.response.PipedriveUser;
import com.onlyoffice.docspacepipedrive.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;


@Component
@RequiredArgsConstructor
public class PipedriveClient {
    private final WebClient pipedriveWebClient;

    public PipedriveDeal getDeal(Long id) {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/deals/{id}")
                        .build(id)
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveDeal>>() {})
                .map(PipedriveResponse<PipedriveDeal>::getData)
                .block();
    }

    public PipedriveUser getUser() {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/users/me")
                        .build()
                        .toUri()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveUser>>() {})
                .map(PipedriveResponse<PipedriveUser>::getData)
                .block();
    }

    private String getBaseUrl() {
        return SecurityUtils.getCurrentUser()
                .getClient()
                .getUrl();
    }
}
