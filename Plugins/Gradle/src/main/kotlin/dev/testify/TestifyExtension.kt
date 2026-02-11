/*
 * The MIT License (MIT)
 *
 * Modified work copyright (c) 2022-2024 ndtp
 * Original work copyright (c) 2019 Shopify Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.testify

import dev.testify.internal.VariantPackageIdStore
import dev.testify.internal.android
import dev.testify.internal.inferredAndroidTestInstallTask
import dev.testify.internal.inferredInstallTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal data class TestifySettings(

    val baselineSourceDir: String,
    val moduleName: String,
    val outputFileNameFormat: String?,
    val pullWaitTime: Long = 0L,
    val testRunner: String,

    /**
     * Allows Testify to write screenshots to the SDCARD directory.
     *
     * @see [https://ndtp.github.io/android-testify/docs/recipes/sdcard#configuring-the-gradle-plugin-to-write-to-the-sdcard]
     */
    val useSdCard: Boolean,

    /**
     * Instructs Testify to save screenshots to the Test Storage.
     *
     * @see [https://developer.android.com/reference/androidx/test/services/storage/TestStorage]
     */
    val useTestStorage: Boolean,

    /**
     * The package ID for the test APK (resolved at execution time via onVariants API when inferred).
     *
     * For a typical application, testify requires two APKs: the target apk under test,
     * and a test apk containing your tests.
     *
     * e.g. com.testify.example.test
     */
    val testPackageIdProvider: Provider<String>,

    /**
     * The package ID for the APK under test (resolved at execution time via onVariants API when inferred).
     *
     * For a typical application, testify requires two APKs: the target apk under test,
     * and a test apk containing your tests.
     *
     * e.g. com.testify.example
     */
    val targetPackageIdProvider: Provider<String>,
    val installTask: String?,
    val installAndroidTestTask: String?,
    val autoImplementLibrary: Boolean = true,

    /**
     * The annotation used by ScreenshotTestTask as an argument to `adb shell am instrument`
     * to filter tests being run.
     *
     * [@see https://developer.android.com/studio/test/command-line#run-tests-with-adb]
     *
     * If null, then will default to dev.testify.annotation.ScreenshotInstrumentation
     */
    val screenshotAnnotation: String? = null,

    /**
     * Allows you to override the root directory used when pulling files from the device
     */
    val rootDestinationDirectory: String? = null,

    /**
     * Allows to record new baseline images.
     */
    val isRecordMode: Boolean,

    /**
     * Gets the number of threads to create in the parallel pixel processor thread pool.
     * If parallelThreads is not set then defaults to the number of available CPU cores.
     * If parallelThreads is less than 1, then defaults to the number of available CPU cores.
     * Minimum is 1.
     * Maximum is 4.
     */
    val parallelThreads: Int? = null
) {

    internal companion object {
        fun create(project: Project): TestifySettings {
            val android = project.android
            val extension = project.getTestifyExtension()

            val baselineSourceDir = extension.baselineSourceDir
                ?: project.android.sourceSets.findByName("androidTest")?.assets?.directories?.firstOrNull()
                ?: "src/androidTest/assets"
            val testRunner = extension.testRunner ?: android.defaultConfig.testInstrumentationRunner ?: "unknown"
            val pullWaitTime = extension.pullWaitTime ?: 0L
            val testPackageIdProvider = extension.testPackageId?.let { project.provider { it } }
                ?: VariantPackageIdStore.getTestPackageIdProvider(project)
                ?: project.provider { "" }
            val targetPackageIdProvider = extension.applicationPackageId?.let { project.provider { it } }
                ?: VariantPackageIdStore.getApplicationPackageIdProvider(project)
                ?: project.provider { "" }
            val version = TestifySettings::class.java.getPackage().implementationVersion
            val isSnapshot = version?.contains("SNAPSHOT", ignoreCase = true) ?: false
            val autoImplementLibrary = extension.autoImplementLibrary ?: !isSnapshot
            val useSdCard = extension.useSdCard ?: false
            val useTestStorage = extension.useTestStorage ?: false
            val installTask = extension.installTask ?: project.inferredInstallTask
            val outputFileNameFormat = extension.outputFileNameFormat
            val installAndroidTestTask = extension.installAndroidTestTask ?: project.inferredAndroidTestInstallTask
            val moduleName = extension.moduleName ?: project.name
            val screenshotAnnotation = extension.screenshotAnnotation
            val rootDestinationDirectory = extension.rootDestinationDirectory
            val isRecordMode = extension.isRecordMode ?: false
            val parallelThreads = extension.parallelThreads ?: 0

            return TestifySettings(
                baselineSourceDir = baselineSourceDir,
                moduleName = moduleName,
                outputFileNameFormat = outputFileNameFormat,
                pullWaitTime = pullWaitTime,
                testRunner = testRunner,
                useSdCard = useSdCard,
                useTestStorage = useTestStorage,
                testPackageIdProvider = testPackageIdProvider,
                targetPackageIdProvider = targetPackageIdProvider,
                installTask = installTask,
                installAndroidTestTask = installAndroidTestTask,
                autoImplementLibrary = autoImplementLibrary,
                screenshotAnnotation = screenshotAnnotation,
                rootDestinationDirectory = rootDestinationDirectory,
                isRecordMode = isRecordMode,
                parallelThreads = parallelThreads
            )
        }
    }

    fun validate(project: Project) {
        val extension = project.getTestifyExtension()
        val (propertyName, examplePackage) = when {
            extension.applicationPackageId?.isEmpty() == true -> "applicationPackageId" to "com.example.app"
            extension.testPackageId?.isEmpty() == true -> "testPackageId" to "com.example.app.test"
            else -> null to null
        }

        propertyName?.let {
            val message = """

  You must define `$it` in your `testify` gradle extension block:

      testify {
          $it "$examplePackage"
      }
      """
            throw GradleExtensionException(message)
        }
    }
}

open class TestifyExtension {

    var baselineSourceDir: String? = null
    var moduleName: String? = null
    var outputFileNameFormat: String? = null
    var pullWaitTime: Long? = null
    var testRunner: String? = null
    var useSdCard: Boolean? = null
    var useTestStorage: Boolean? = null
    var applicationPackageId: String? = null
    var testPackageId: String? = null
    var installTask: String? = null
    var installAndroidTestTask: String? = null
    var autoImplementLibrary: Boolean? = null
    var screenshotAnnotation: String? = null
    var rootDestinationDirectory: String? = null
    var isRecordMode: Boolean? = null
    var parallelThreads: Int? = null

    companion object {
        const val NAME = "testify"
    }
}

fun Project.validateExtension() {
    if (this.extensions.findByName(TestifyExtension.NAME) == null) {
        throw GradleExtensionException("You must define a `testify` extension block in your build.gradle")
    }
}

fun Project.getTestifyExtension(): TestifyExtension = this.extensions.findByType(TestifyExtension::class.java)!!

data class GradleExtensionException(override val message: String) : GradleException(message)
