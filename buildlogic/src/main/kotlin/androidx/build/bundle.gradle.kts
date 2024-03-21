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

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    kotlin("jvm")
}

val bundleInside: Configuration by project.configurations.creating {
//            isTransitive = false
    isVisible = false
    isCanBeResolved = false
    isCanBeConsumed = false
    isCanBeDeclared = true
}

val bundleInsideResolver: Configuration by project.configurations.creating {
    extendsFrom(bundleInside)
    isTransitive = false
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    isCanBeDeclared = false
}


project.configurations.getByName("compileOnly").extendsFrom(bundleInside)
//            project.configurations.getByName("testImplementation").extendsFrom(bundleInside)

project.tasks.named<Jar>("jar").configure {
    val archives = project.serviceOf<ArchiveOperations>()
    val classesToBundle = bundleInsideResolver.incoming.artifacts.resolvedArtifacts.map {
        it.map(ResolvedArtifactResult::getFile)
            .map { f ->
                if (f.isDirectory) f else archives.zipTree(f)
            }
    }
    from(classesToBundle)
}

project.tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(bundleInsideResolver)
}
