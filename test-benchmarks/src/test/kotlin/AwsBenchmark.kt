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

import androidx.build.gradle.s3buildcache.S3StorageService
import com.adobe.testing.s3mock.S3MockApplication
import com.adobe.testing.s3mock.S3MockApplication.*
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream
import kotlin.random.Random
import kotlinx.benchmark.*


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 500, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = BenchmarkTimeUnit.SECONDS)
@State(Scope.Benchmark)
abstract class AwsBenchmark {
    private var s3MockApplication: S3MockApplication? = null
    private var client: S3Client? = null
    private var storageService: S3StorageService? = null

    @Setup(Level.Invocation)
    fun setUp() {
        val serverConfig = mapOf(
            PROP_INITIAL_BUCKETS to BUCKET_NAME,
            PROP_SILENT to true
        )
        s3MockApplication = start(serverConfig)
//        val serviceEndpoint = "http://localhost:$DEFAULT_HTTP_PORT"
        client = S3Client.builder()
            .region(Region.of(REGION))
            .credentialsProvider(AnonymousCredentialsProvider.create())
//            .endpointOverride(URI.create(serviceEndpoint))
            .forcePathStyle(true) // https://github.com/adobe/S3Mock/issues/880
            .build()

        storageService = S3StorageService(
            region = REGION,
            bucketName = BUCKET_NAME,
            client = client!!,
            isPush = true,
            isEnabled = true,
            reducedRedundancy = true,
            sizeThreshold = SIZE_THRESHOLD,
        )
    }

    @TearDown
    fun tearDown() {
        storageService?.close()
        s3MockApplication?.stop()
        client?.close()
    }

    @Benchmark
    fun storeRandomData(bh: Blackhole) {
        val cacheKey = "test-store.txt"

        val randomStream = object : InputStream() {
            override fun read(): Int =
                Random(1).nextInt()
        }

        storageService?.store(cacheKey, randomStream, 10 * 1024 * 1024)
        storageService?.delete(cacheKey)
    }

//    @Benchmark
//    fun fibTailRec(bh: Blackhole) {
////        bh.consume(Fib.tailRecFib(30))
//    }

    companion object {
        private const val REGION = "us-east-1"
        private const val BUCKET_NAME = "bucket-name"
        private const val SIZE_THRESHOLD = 50 * 1024 * 1024L
    }
}
