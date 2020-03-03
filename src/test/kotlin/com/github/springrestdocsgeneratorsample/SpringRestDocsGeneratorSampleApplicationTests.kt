package com.github.springrestdocsgeneratorsample

import com.github.embedded.gradle.generateBuild
import com.github.kotlinpoetdsl.kotlinFile
import com.squareup.kotlinpoet.KModifier
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation
import org.springframework.test.web.reactive.server.WebTestClient
import java.nio.file.Files
import java.nio.file.Path

class SpringRestDocsGeneratorSampleApplicationTests {

	@TempDir
	lateinit var tempDir: Path

	@Test
	fun `generate rest docs for sample endpoint with kotlin poet and embedded gradle`() {
		val gradleBuild = generateBuild(tempDir) {

			settingsFile {
				"""
                rootProject.name = "hello-world-rest-docs-test"
            """.trimIndent()
			}

			buildFile {
				"""
                plugins {
                    id "org.asciidoctor.convert" version "1.5.9.2"
                    id "org.jetbrains.kotlin.jvm" version "1.3.61"
                }
                
                repositories {
                    mavenLocal()
                    mavenCentral()
                }
                
                ext {
                    snippetsDir = file('build/generated-snippets')
                }
                
                dependencies {
                    compile 'org.jetbrains.kotlin:kotlin-reflect'
                    compile 'org.jetbrains.kotlin:kotlin-stdlib-jdk8'
                
                    testCompile 'org.junit.jupiter:junit-jupiter-api:5.6.0'
                    testRuntime 'org.junit.jupiter:junit-jupiter-engine:5.6.0'
                    testCompile 'org.springframework:spring-test:5.2.4.RELEASE'
                    testCompile 'org.springframework:spring-webflux:5.2.4.RELEASE'
                    testCompile 'io.projectreactor.netty:reactor-netty:0.9.5.RELEASE'
                    testCompile 'org.springframework.restdocs:spring-restdocs-webtestclient:2.0.4.RELEASE'
                    asciidoctor 'org.springframework.restdocs:spring-restdocs-asciidoctor:2.0.4.RELEASE'
                }
                
                tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
                    kotlinOptions {
                        jvmTarget = "1.8"
                    }
                }
                
                test {
                    outputs.dir snippetsDir
                    useJUnitPlatform()
                }
                
                asciidoctor {
                    inputs.dir snippetsDir
                    dependsOn test
                }
            """.trimIndent()
			}

			addAsciiDocFile("index.adoc") {
				"""
                = Spring REST Docs Generator Sample
                @Author;
                :doctype: book
                :icons: font
                :source-highlighter: highlightjs

                Sample application demonstrating Spring REST Docs generation with KotlinPoet and Gradle.

                `SampleRestDocsApplicationTests` makes a call to 'Hello World' REST service and produces three
                documentation snippets.

                One showing how to make a request using cURL:

                include::{snippets}/sample/curl-request.adoc[]

                One showing the HTTP request:

                include::{snippets}/sample/http-request.adoc[]

                And one showing the HTTP response:

                include::{snippets}/sample/http-response.adoc[]

            """.trimIndent()
			}

			addTestSourceFile {
				kotlinFile("com.restdocs.test", "SampleRestDocsApplicationTests") {
					clazz("SampleRestDocsApplicationTests") {
						annotation(ExtendWith::class) {
							addMember("%T::class", RestDocumentationExtension::class)
						}
						property("webTestClient", WebTestClient::class) {
							addModifiers(KModifier.LATEINIT)
							mutable()
						}
						function("setUp") {
							annotation(BeforeEach::class)
							parameter("restDocumentation", RestDocumentationContextProvider::class)
							body {
								addStatement(
										"""
                                        this.webTestClient = WebTestClient.bindToServer()
                                            .baseUrl("http://localhost:8080")
                                            .filter(%T.documentationConfiguration(restDocumentation))
                                            .build();
                                    """.trimIndent(), WebTestClientRestDocumentation::class)
							}
						}
						function("test hello world endpoint") {
							annotation(Test::class)
							body {
								addStatement("""
                                    this.webTestClient.get().uri("/").exchange()
                                        .expectStatus().isOk().expectBody()
                                    	.consumeWith(WebTestClientRestDocumentation.document("sample"));
                                """.trimIndent())
							}
						}
					}
				}
			}

			args("asciidoctor")
		}

		val result = gradleBuild.runner().build()
		Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":asciidoctor")!!.outcome)

		Files.copy(tempDir.resolve(Path.of("build", "asciidoc", "html5", "index.html")), Path.of("asciidoc.html"))
	}
}
