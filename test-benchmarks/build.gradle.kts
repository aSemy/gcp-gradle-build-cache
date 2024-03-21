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

plugins {
    id("androidx.build.kotlin-jvm")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.10"
    kotlin("plugin.allopen") version embeddedKotlinVersion
}

dependencies {
    implementation(project(":core"))
    implementation(project(":s3buildcache"))
    implementation(project(":gcpbuildcache"))
    implementation(libs.adobe.s3.mock) {
        // Classpath collisions
        exclude("ch.qos.logback", "logback-classic")
    }
    implementation(platform(libs.amazon.bom))
//    implementation(platform(libs.okhttp.bom))
//    implementation(libs.retrofit.core)
//    implementation(libs.retrofit.converter.gson)
//    implementation(libs.google.gson)
//    implementation(libs.okhttp)
    implementation(libs.amazon.s3)
    implementation(libs.amazon.sso)

    implementation(testFixtures(project(":core")))

    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

//jmh {
//    warmupIterations = 2
//    iterations = 2
//    fork = 2
//}
//
//tasks.matching { it.group  == "jmh" }.configureEach {
//    notCompatibleWithConfigurationCache("gradle sucks")
//}
//
//tasks.jmhJar {
//    isZip64 = true
//}

benchmark {
    targets {
        register("main")
    }
    configurations {
        named("main") {
            warmups = 3
            iterations = 10
            iterationTime = 3
            iterationTimeUnit = "s"
//            advanced("jvmProfiler", "gc")
        }
//        register("smoke") {
//            include("<pattern of fully qualified name>")
//            warmups = 5
//            iterations = 3
//            iterationTime = 500
//            iterationTimeUnit = "ms"
//        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}
