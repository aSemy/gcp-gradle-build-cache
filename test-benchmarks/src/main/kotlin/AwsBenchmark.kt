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
package androidx.build.gradle.core

import androidx.build.gradle.s3buildcache.DefaultS3Credentials
import androidx.build.gradle.s3buildcache.S3BuildCacheService
import com.adobe.testing.s3mock.S3MockApplication
import com.adobe.testing.s3mock.S3MockApplication.*
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Blackhole
import org.gradle.caching.BuildCacheEntryWriter
import org.gradle.caching.BuildCacheKey
import org.openjdk.jmh.annotations.*
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.OutputStream
import java.net.URI


@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 30, timeUnit = BenchmarkTimeUnit.SECONDS)
class AwsBenchmark {

    @Param("byte-array", "piped", "buffer")
    var mode: String = ""

    private final val s3MockApplication: S3MockApplication
    private final val client: S3Client
    private var buildCache: S3BuildCacheService? = null

    init {
        val port = randomPort()
        val serverConfig = mapOf(
            PROP_INITIAL_BUCKETS to BUCKET_NAME,
            PROP_SILENT to true,
            PROP_HTTP_PORT to port,
            PROP_HTTPS_PORT to RANDOM_PORT,
        )
        s3MockApplication = start(serverConfig)
        println("running S3MockApplication on port $port")

        client = S3Client.builder()
            .region(Region.of(REGION))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .endpointOverride(URI.create("http://localhost:$port"))
            .forcePathStyle(true) // https://github.com/adobe/S3Mock/issues/880
            .build()
    }

    @Setup(Level.Invocation)
    fun setUp() {
        buildCache = S3BuildCacheService(
            credentials = DefaultS3Credentials,
            region = REGION,
            bucketName = BUCKET_NAME,
            isPush = true,
            reducedRedundancy = true,
            inTestMode = false,
            mode = mode,
        )
    }

    @TearDown
    fun tearDown() {
        buildCache?.close()
    }

    @Benchmark
    fun storeRandomData(bh: Blackhole) {
        buildCache!!.store(
            object : BuildCacheKey {
                override fun getDisplayName(): String = cacheKey
                override fun getHashCode(): String = cacheKey
                override fun toByteArray(): ByteArray = cacheKey.encodeToByteArray()
            },
            object : BuildCacheEntryWriter {
                override fun writeTo(output: OutputStream) {
                    repeat(sourceDataSize) {
                        output.write(it)
                    }
                }

                override fun getSize(): Long = sourceDataSize.toLong()
            }
        )
    }

    companion object {
        private const val cacheKey = "test-store.txt"

        private val sourceDataSize = 10 * 1024 * 1024 // 10MB

        private const val REGION = "us-east-1"
        private const val BUCKET_NAME = "bucket-name"
    }
}
