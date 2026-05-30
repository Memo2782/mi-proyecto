package com.ejemplo.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;

import java.time.OffsetDateTime;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.ejemplo.repository")
public class AppConfig extends AbstractReactiveMongoConfiguration implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

    @Value("${mongodbUri}")
    private String mongoUri;

    @Value("${mongodbDatabase}")
    private String databaseName;

    @Value("${apiPort}")
    private int apiPort;

    @Override
    protected String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    @Override
    public ReactiveMongoDatabaseFactory reactiveMongoDbFactory() {
        return new SimpleReactiveMongoDatabaseFactory(reactiveMongoClient(), getDatabaseName());
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDbFactory, MappingMongoConverter mappingMongoConverter) {
        return new ReactiveMongoTemplate(reactiveMongoDbFactory, mappingMongoConverter);
    }

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        factory.setPort(apiPort);
    }

    @Override
    protected void configureConverters(MongoCustomConversions.MongoConverterConfigurationAdapter adapter) {
        adapter.registerConverter(new OffsetDateTimeToStringConverter());
        adapter.registerConverter(StringToOffsetDateTimeConverter.INSTANCE);
    }

    private static class OffsetDateTimeToStringConverter implements Converter<OffsetDateTime, String> {
        @Override
        public String convert(OffsetDateTime source) {
            return source.toString();
        }
    }

    private enum StringToOffsetDateTimeConverter implements Converter<String, OffsetDateTime> {
        INSTANCE;
        @Override
        public OffsetDateTime convert(String source) {
            return OffsetDateTime.parse(source);
        }
    }
}
