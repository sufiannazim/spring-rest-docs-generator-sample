package com.github.springrestdocsgeneratorsample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class SpringRestDocsGeneratorSampleApplication

fun main(args: Array<String>) {
	runApplication<SpringRestDocsGeneratorSampleApplication>(*args)
}

@RestController
class SampleRESTController {

	@GetMapping
	fun helloWorld() : String {
		return "Hello World!"
	}
}