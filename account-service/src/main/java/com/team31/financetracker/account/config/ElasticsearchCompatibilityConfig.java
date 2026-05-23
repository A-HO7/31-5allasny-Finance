package com.team31.financetracker.account.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.net.URI;

/**
 * Configures the Elasticsearch Java API client with backward-compatibility headers.
 *
 * Spring Boot 4 bundles co.elastic.clients 9.x which uses the ES 9 wire format.
 * The cluster runs ES 8.19.  Without the compatibility headers, the 9.x client
 * triggers HTTP 400 on HEAD /index existence checks, crashing repository startup.
 *
 * Solution: inject the versioned Accept / Content-Type media-type headers into
 * every request so ES 8.x accepts them in compatibility mode, then expose a
 * primary ElasticsearchClient + ElasticsearchOperations so Spring Data ES uses
 * our correctly configured client instead of the auto-configured one.
 */
@Configuration
public class ElasticsearchCompatibilityConfig {

    /** Compatibility media-type accepted by ES 8.x when talking to a 9.x client */
    private static final String ES8_COMPAT_MIME =
            "application/vnd.elasticsearch+json;compatible-with=8";

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        URI uri = URI.create(elasticsearchUri);

        // Low-level async Rest5Client backed by Apache HttpClient 5
        Rest5Client restClient = Rest5Client
                .builder(new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort()))
                .build();

        // Build request options that add backward-compat headers on every request.
        // RequestOptions.DEFAULT.toBuilder() is the only public way to get a Builder.
        RequestOptions compatOptions = RequestOptions.DEFAULT.toBuilder()
                .addHeader("Accept", ES8_COMPAT_MIME)
                .addHeader("Content-Type", ES8_COMPAT_MIME)
                .build();

        // Rest5ClientOptions wraps RequestOptions; second arg = keepResponseBodyOnException
        Rest5ClientOptions rest5Options = new Rest5ClientOptions(compatOptions, true);

        ElasticsearchTransport transport = new Rest5ClientTransport(
                restClient,
                new JacksonJsonpMapper(),
                rest5Options);

        return new ElasticsearchClient(transport);
    }

    @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
    @Primary
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchClient client) {
        return new ElasticsearchTemplate(client);
    }
}
