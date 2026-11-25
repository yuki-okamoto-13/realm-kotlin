/*
 * Copyright 2020 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.kotlin.gradle

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RealmCompilerSubplugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        // TODO LATER Consider embedding these from the build.gradle's versionConstants task just
        //  as with the version. But leave it for now as they should be quite stable.
        // Modules has to match ${project.group}:${project.name} to make composite build work
        const val groupId = "yuki.okamoto.realm.kotlin"
        const val artifactId = "plugin-compiler"
        const val version = PLUGIN_VERSION

        // The id used for passing compiler options from command line
        const val compilerPluginId = "yuki.okamoto.realm.kotlin"

        // `pluginId`s of the compiler plugins' `CompilerPluginRegistrar`s, used for ordering them.
        private const val REALM_COMPILER_PLUGIN_ID = "io.realm.kotlin"
        private const val SERIALIZATION_COMPILER_PLUGIN_ID = "org.jetbrains.kotlinx.serialization"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        return kotlinCompilation.target.project.plugins.findPlugin(RealmCompilerSubplugin::class.java) != null
    }

    override fun getCompilerPluginId(): String {
        return compilerPluginId
    }

    override fun getPluginArtifact(): SubpluginArtifact {
        // Modules has to match ${project.group}:${project.name} to make composite build work
        return SubpluginArtifact(groupId, artifactId, version)
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        // Run our compiler plugin after kotlinx.serialization. Otherwise the serialization plugin
        // sees our internal attributes and the compilation of `@Serializable` model classes fails
        // (see https://github.com/realm/realm-kotlin/issues/1251). The ids are the `pluginId`s of
        // the respective `CompilerPluginRegistrar`s. The constraint is ignored if serialization
        // is not applied.
        kotlinCompilation.compileTaskProvider.configure {
            it.compilerOptions.freeCompilerArgs.add(
                "-Xcompiler-plugin-order=$SERIALIZATION_COMPILER_PLUGIN_ID>$REALM_COMPILER_PLUGIN_ID"
            )
        }

        return project.provider { emptyList() }
    }
}
