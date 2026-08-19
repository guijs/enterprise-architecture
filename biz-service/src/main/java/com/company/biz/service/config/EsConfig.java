package com.company.biz.service.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch Java Client（8.x）装配。统一使用官方 client，禁止 TransportClient / RestHighLevelClient。
 */
@Configuration(proxyBeanMethods = false)
public class EsConfig {

    @Value("${es.host:localhost}")
    private String host;

    @Value("${es.port:9200}")
    private int port;

    @Value("${es.scheme:http}")
    private String scheme;

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port, scheme)).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
