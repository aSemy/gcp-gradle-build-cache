/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.gradle.gcpbuildcache

import androidx.build.gradle.core.FileSystemStorageService
import androidx.build.gradle.core.blobKey
import org.gradle.api.logging.Logging
import org.gradle.caching.BuildCacheEntryReader
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.gradle.caching.BuildCacheService
import java.io.ByteArrayOutputStream

/**
 * The service that responds to Gradle's request to load and store results for a given
 * [BuildCacheKey].
 *
 * @param projectId The Google Cloud Platform project id, that can be used for billing.
 * @param bucketName The name of the bucket that is used to store all the gradle cache entries.
 * This essentially becomes the root of all cache entries.
 */
internal class GcpBuildCacheService(
    private val projectId: String,
    private val bucketName: String,
    gcpCredentials: GcpCredentials,
    messageOnAuthenticationFailure: String,
    isPush: Boolean,
    isEnabled: Boolean,
    inTestMode: Boolean = false
) : BuildCacheService {

    private val storageService = if (inTestMode) {
        // Use an implementation backed by the File System when in test mode.
        FileSystemStorageService(bucketName, isPush, isEnabled)
    } else {
        GcpStorageService(projectId, bucketName, gcpCredentials, messageOnAuthenticationFailure, isPush, isEnabled)
    }

    override fun close() {
        // Does nothing
    }

    override fun load(key: BuildCacheKey, reader: BuildCacheEntryReader): Boolean {
        logger.info("Loading ${key.blobKey()}")
        val cacheKey = key.blobKey()
        val input = storageService.load(cacheKey) ?: return false
        reader.readFrom(input)
        return true
    }

    override fun store(key: BuildCacheKey, writer: BuildCacheEntryWriter) {
        if (writer.size == 0L) return // do not store empty entries into the cache
        logger.info("Storing ${key.blobKey()}")
        val cacheKey = key.blobKey()
        val output = ByteArrayOutputStream()
        output.use {
            writer.writeTo(output)
        }
        storageService.store(cacheKey, output.toByteArray())
    }

    fun validateConfiguration() {
        storageService.validateConfiguration()
    }

    companion object {

        private val logger by lazy {
            Logging.getLogger("GcpBuildCacheService")
        }
    }
}
