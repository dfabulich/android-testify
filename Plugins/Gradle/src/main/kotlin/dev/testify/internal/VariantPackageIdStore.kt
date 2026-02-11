/*
 * The MIT License (MIT)
 *
 * Modified work copyright (c) 2022-2024 ndtp
 * Original work copyright (c) 2019 Shopify Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and the associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the requirements of the following condition:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING WARRANTIES OF CONDITIONS OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package dev.testify.internal

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import org.gradle.api.Project
import org.gradle.api.provider.Provider

/**
 * Collects package IDs from the onVariants API for execution-time resolution.
 *
 * Original behavior: first debug app variant (sorted by name) for applicationId,
 * first test variant (sorted by testedVariant.flavorName) for test package ID.
 * We use selector().withBuildType("debug") and collect all debug variants,
 * then at execution time pick first by name (app) and first by flavorName (test).
 */
internal object VariantPackageIdStore {

    private val appPackageIds = mutableMapOf<Project, MutableList<Pair<String, Provider<String>>>>()
    private val testPackageIds = mutableMapOf<Project, MutableList<Pair<String, Provider<String>>>>()

    fun register(project: Project) {
        val appComponents = project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
        val libComponents = project.extensions.findByType(LibraryAndroidComponentsExtension::class.java)

        appComponents?.let { components ->
            appPackageIds[project] = mutableListOf()
            testPackageIds[project] = mutableListOf()
            components.onVariants(components.selector().withBuildType("debug")) { variant ->
                val appVariant = variant as ApplicationVariant
                appPackageIds[project]!!.add(variant.name to appVariant.applicationId)
                val androidTest = appVariant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]
                if (androidTest != null) {
                    val flavorKey = appVariant.flavorName ?: ""
                    testPackageIds[project]!!.add(flavorKey to androidTest.applicationId)
                }
            }
        }

        libComponents?.let { components ->
            if (!testPackageIds.containsKey(project)) {
                testPackageIds[project] = mutableListOf()
            }
            components.onVariants(components.selector().withBuildType("debug")) { variant ->
                val libVariant = variant as LibraryVariant
                val androidTest = libVariant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]
                if (androidTest != null) {
                    val flavorKey = libVariant.flavorName ?: ""
                    testPackageIds[project]!!.add(flavorKey to androidTest.applicationId)
                }
            }
        }
    }

    fun getApplicationPackageIdProvider(project: Project): Provider<String>? {
        val entries = appPackageIds[project] ?: return null
        return project.provider {
            entries.minByOrNull { it.first }?.second?.get() ?: ""
        }
    }

    fun getTestPackageIdProvider(project: Project): Provider<String>? {
        val entries = testPackageIds[project] ?: return null
        return project.provider {
            entries.minByOrNull { it.first }?.second?.get() ?: ""
        }
    }
}
