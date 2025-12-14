package com.momentum.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MomentumApplication

fun main(args: Array<String>) {
    runApplication<MomentumApplication>(*args)
}
