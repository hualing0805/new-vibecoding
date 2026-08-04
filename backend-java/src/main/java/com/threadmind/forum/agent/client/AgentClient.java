package com.threadmind.forum.agent.client;

import com.threadmind.forum.common.web.TraceContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AgentClient {

    private final AgentProperties properties;
    private final RestClient.Builder restClientBuilder;

    public AgentClient(AgentProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public <T> T post(String path, Object request, Class<T> responseType) {
        RestClient restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("X-Internal-Token", properties.getInternalToken())
                .defaultHeader(TraceContext.TRACE_ID_HEADER, TraceContext.currentTraceId())
                .build();

        return restClient
                .post()
                .uri(path)
                .body(request)
                .retrieve()
                .body(responseType);
    }
}

