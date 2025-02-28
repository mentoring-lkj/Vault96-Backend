package com.dev.vault96.config.db;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Collation;

@Configuration
public class MongoConfig {

    @Bean
    public Collation createCollation(){
        return Collation.of("ko")

                .strength(Collation.ComparisonLevel.secondary()
                        .includeCase())

                .numericOrderingEnabled()

                .alternate(Collation.Alternate.shifted().punct())

                .forwardDiacriticSort()

                .normalizationEnabled();
    }
}
