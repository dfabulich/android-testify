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

package dev.testify.internal

import dev.testify.internal.StreamData.ConsoleStream
import dev.testify.internal.Style.Description
import dev.testify.testifySettings
import org.gradle.api.Project
import java.io.File

internal fun Project.root(targetPackageId: String): String =
    computeRoot(targetPackageId, testifySettings.useSdCard, testifySettings.rootDestinationDirectory)

internal fun Project.screenshotDirectory(targetPackageId: String): String =
    computeScreenshotDirectory(targetPackageId, testifySettings.useSdCard, testifySettings.rootDestinationDirectory)

/**
 * Pure functions for path computation - safe to call at task execution time without Project.
 * Used for configuration cache compatibility.
 */
internal fun computeRoot(
    targetPackageId: String,
    useSdCard: Boolean,
    rootDestinationDirectory: String?
): String {
    @Suppress("SdCardPath")
    return rootDestinationDirectory ?: if (useSdCard) {
        "/sdcard/Android/data/$targetPackageId/files/testify_"
    } else {
        "./app_"
    }
}

internal fun computeScreenshotDirectory(
    targetPackageId: String,
    useSdCard: Boolean,
    rootDestinationDirectory: String?
): String {
    val r = computeRoot(targetPackageId, useSdCard, rootDestinationDirectory)
    return if (useSdCard) {
        "${r}images/"
    } else {
        "${r}images/$SCREENSHOT_DIR"
    }
}

internal fun computeReportFilePath(
    targetPackageId: String,
    useSdCard: Boolean,
    rootDestinationDirectory: String?
): String =
    computeRoot(targetPackageId, useSdCard, rootDestinationDirectory).replace("testify_", "") + "testify"

internal fun Adb.listFiles(path: String): List<String> {
    val log = this
        .argument("ls $path")
        .argument("2>/dev/null")
        .execute()

    return log.lines().filter { it.isNotEmpty() }.map { "$path/$it" }
}

internal fun listFailedScreenshotsWithPath(src: String, targetPackageId: String, isVerbose: Boolean): List<String> {
    val rootDir = Adb()
        .shell()
        .runAs(targetPackageId)
        .listFiles(src)
    val files = rootDir.flatMap {
        Adb()
            .shell()
            .runAs(targetPackageId)
            .listFiles(it)
    }

    if (isVerbose) {
        files.forEach { println(Description, "\t$it") }
    }
    return files
}

internal fun listFailedScreenshots(
    src: String,
    dst: String,
    targetPackageId: String,
    isVerbose: Boolean
): List<String> {
    val files = listFailedScreenshotsWithPath(
        src = src,
        targetPackageId = targetPackageId,
        isVerbose = isVerbose
    )
    return files.map { it.replace(src, dst) }
}

internal fun Project.reportFilePath(targetPackageId: String): String =
    computeReportFilePath(targetPackageId, testifySettings.useSdCard, testifySettings.rootDestinationDirectory)

internal fun File.deleteOnDevice(targetPackageId: String) {
    Adb()
        .shell()
        .runAs(targetPackageId)
        .argument("rm")
        .argument(this.path.unixPath)
        .stream(ConsoleStream)
        .execute()
}

private val String.unixPath: String
    get() = this.replace('\\', '/')

const val SCREENSHOT_DIR = "screenshots"
