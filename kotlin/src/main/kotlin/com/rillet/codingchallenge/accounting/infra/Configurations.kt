package com.rillet.codingchallenge.accounting.infra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.jackson.datatype.money.MoneyModule

@Configuration
class Configurations {
  @Bean
  fun objectMapper(): ObjectMapper =
      jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        registerModule(MoneyModule().withAmountFieldName("value").withQuotedDecimalNumbers())
      }
}
