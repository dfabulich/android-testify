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
package dev.testify.tasks.main

import dev.testify.internal.Style.Failure
import dev.testify.internal.Style.FailureHeader
import dev.testify.internal.Style.Success
import dev.testify.internal.computeScreenshotDirectory
import dev.testify.internal.isVerbose
import dev.testify.testifySettings
import dev.testify.internal.deleteOnDevice
import dev.testify.internal.listFailedScreenshotsWithPath
import dev.testify.internal.println
import dev.testify.tasks.internal.TaskNameProvider
import dev.testify.tasks.internal.TestifyDefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

open class ScreenshotClearTask : TestifyDefaultTask() {

    @get:Input
    var isVerbose: Boolean = false

    @get:Input
    var useSdCard: Boolean = false

    @get:Optional
    @get:Input
    var rootDestinationDirectory: String? = null

    override fun getDescription() = "Remove any existing screenshot test images from the device"

    private val targetPackageIdProperty: Property<String> =
        project.objects.property(String::class.java)

    override fun provideInput(project: Project) {
        super.provideInput(project)
        targetPackageIdProperty.set(project.testifySettings.targetPackageIdProvider)
        inputs.property("targetPackageId", targetPackageIdProperty)
        isVerbose = project.isVerbose
        useSdCard = project.testifySettings.useSdCard
        rootDestinationDirectory = project.testifySettings.rootDestinationDirectory
    }

    override fun taskAction() {
        val targetPackageId = targetPackageIdProperty.get()
        val screenshotDirectory = computeScreenshotDirectory(targetPackageId, useSdCard, rootDestinationDirectory)
        val failedScreenshots = listFailedScreenshotsWithPath(
            src = screenshotDirectory,
            targetPackageId = targetPackageId,
            isVerbose = isVerbose
        )

        if (failedScreenshots.isEmpty()) {
            println(Success, "  No failed screenshots found")
            return
        }

        println(FailureHeader, "  ${failedScreenshots.size} images to be deleted:")
        failedScreenshots.forEach {
            val file = File(it)
            println(Failure, "    x ${file.nameWithoutExtension}")
            file.deleteOnDevice(targetPackageId)
        }
    }

    companion object : TaskNameProvider {
        override fun taskName() = "screenshotClear"
    }
}
