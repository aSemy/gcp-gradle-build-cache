/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package androidx.build

import org.gradle.api.Task
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.Sign

plugins {
    id("androidx.build.bundle")
    id("androidx.build.kotlin-jvm")
    id("com.gradle.plugin-publish")
    id("maven-publish")
    id("signing")
    `jvm-test-suite`
}

testing {
    suites {
        // Configure built-in test suite.
        val test by getting(JvmTestSuite::class) {
            useJUnit()
        }

        // Create a new functional test suite.
        val functionalTest by registering(JvmTestSuite::class) {
            useJUnit()

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project())
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}

tasks.withType<Sign>().configureEach {
    val signingKeyIdPresent = project.hasProperty("signing.keyId")
    onlyIf("signing.keyId is present") { signingKeyIdPresent }
}
