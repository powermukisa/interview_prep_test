package com.rillet.codingchallenge.accounting

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class AccountingApplication

fun main(args: Array<String>) {
    SpringApplication.run(AccountingApplication::class.java, *args)
}