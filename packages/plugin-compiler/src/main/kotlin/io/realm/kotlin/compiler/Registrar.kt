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

package io.realm.kotlin.compiler

import com.google.auto.service.AutoService
import io.realm.kotlin.compiler.fir.model.RealmModelRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/**
 * Registrar for the Realm compiler plugin.
 *
 * The overall concepts of the compiler plugin is that it:
 * - Adds [RealmObjectInternal] interface to all classes marked with [RealmObject] interface
 * - Rewire accessors to the actual Realm for managed objects
 * - Adds [RealmObjectCompanion] interface to the companion object of classes marked with
 * [RealmObject] interface
 * - Modify [RealmConfiguration] constructor calls to capture the companion objects of supplied
 * schema classes.
 *
 * The [RealmObjectInternal] holds internal attributes like Realm and objects native pointer, type
 * information, etc. This information is used to indicate if an object is managed or not and direct
 * the accessors to the Realm if so.
 *
 * The [RealmObjectCompanion] holds static information about the schema (members, primary key, etc.)
 * and utility methods for constructing objects, etc.
 */
@AutoService(CompilerPluginRegistrar::class)
@OptIn(ExperimentalCompilerApi::class)
class Registrar : CompilerPluginRegistrar() {

    // Must match RealmCommandLineProcessor.pluginId. Also used by `-Xcompiler-plugin-order`
    // (see RealmCompilerSubplugin) to make sure our extensions run after kotlinx.serialization,
    // so our internal attributes are not exposed to it (issue https://github.com/realm/realm-kotlin/issues/1251).
    override val pluginId: String = PLUGIN_ID

    override val supportsK2: Boolean = true

    @OptIn(MessageCollectorAccess::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        messageCollector = configuration.messageCollector
        SchemaCollector.properties.clear()

        // Generates companion objects (with RealmObjectCompanion) and the Realm specific
        // toString(), equals() and hashCode() methods in model classes. Kotlin 2.4 only supports
        // the K2 frontend, so the former K1 SyntheticResolveExtensions are no longer needed.
        FirExtensionRegistrarAdapter.registerExtension(RealmModelRegistrar())

        // Adds RealmObjectInternal properties, rewires accessors and adds static companion
        // properties and methods
        IrGenerationExtension.registerExtension(RealmModelLoweringExtension())
    }

    companion object {
        const val PLUGIN_ID = "io.realm.kotlin"
    }
}
