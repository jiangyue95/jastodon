package com.yue.jastodon.federation.note;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;

@Configuration
class NoteConfiguration {

    @Bean
    IdGenerator noteIdGenerator() {
        return new JdkIdGenerator();
    }
}
