package com.booknest.book.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

// Configuration for connecting to the Elasticsearch search engine
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        // Strip protocol from URI to extract host and port
        String hostAndPort = elasticsearchUri
            .replace("http://", "")
            .replace("https://", "");

        return ClientConfiguration.builder()
            .connectedTo(hostAndPort)
            .withConnectTimeout(java.time.Duration.ofSeconds(5))
            .withSocketTimeout(java.time.Duration.ofSeconds(30))
            .build();
    }
}