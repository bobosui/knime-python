/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 25, 2019 (marcel): created
 */
package org.knime.python2.prefs;

import java.util.Collection;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.node.NodeLogger;
import org.knime.python2.Activator;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.config.ManualEnvironmentConfig;
import org.knime.python2.config.PythonVersionConfig;
import org.knime.python2.config.SerializerConfig;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class PythonPreferences {

    static final PreferenceStorage CURRENT = new InstanceScopePreferenceStorage();

    static final PreferenceStorage DEFAULT = new DefaultScopePreferenceStorage();

    private PythonPreferences() {
    }

    /**
     * @return The currently configured default Python version. Either "python2" or "python3".
     */
    public static String getPythonVersionPreference() {
        final PythonVersionConfig pythonVersionConfig = new PythonVersionConfig();
        new PythonVersionPreferencePersistor(pythonVersionConfig).loadSettingsFrom(CURRENT);
        return pythonVersionConfig.getPythonVersion().getStringValue();
    }

    /**
     * @return The currently configured default Python 2 path.
     */
    public static String getPython2CommandPreference() {
        // TODO: Later, we need to check if manual vs. conda environment configuration is active.
        final ManualEnvironmentConfig manualEnvironmentconfig = new ManualEnvironmentConfig();
        new ManualEnvironmentPreferencePersistor(manualEnvironmentconfig).loadSettingsFrom(CURRENT);
        return manualEnvironmentconfig.getPython2Path().getStringValue();
    }

    /**
     * @return The currently configured default Python 3 path.
     */
    public static String getPython3CommandPreference() {
        // TODO: Later, we need to check if manual vs. conda environment configuration is active.
        final ManualEnvironmentConfig manualEnvironmentconfig = new ManualEnvironmentConfig();
        new ManualEnvironmentPreferencePersistor(manualEnvironmentconfig).loadSettingsFrom(CURRENT);
        return manualEnvironmentconfig.getPython3Path().getStringValue();
    }

    /**
     * @return The currently configured serialization library.
     */
    public static String getSerializerPreference() {
        final SerializerConfig serializerConfig = new SerializerConfig();
        new SerializerPreferencePersistor(serializerConfig).loadSettingsFrom(CURRENT);
        return serializerConfig.getSerializer().getStringValue();
    }

    /**
     * @return The required modules of the currently configured serialization library.
     */
    public static Collection<PythonModuleSpec> getCurrentlyRequiredSerializerModules() {
        return SerializationLibraryExtensions.getSerializationLibraryFactory(getSerializerPreference())
            .getRequiredExternalModules();
    }

    private static final class InstanceScopePreferenceStorage implements PreferenceStorage {

        @Override
        public void writeBoolean(final String key, final boolean value) {
            getInstanceScopePreferences().putBoolean(key, value);
            flush();
        }

        @Override
        public boolean readBoolean(final String key, final boolean defaultValue) {
            return Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID, key,
                DEFAULT.readBoolean(key, defaultValue), null);
        }

        @Override
        public void writeString(final String key, final String value) {
            getInstanceScopePreferences().put(key, value);
            flush();
        }

        @Override
        public String readString(final String key, final String defaultValue) {
            return Platform.getPreferencesService().getString(Activator.PLUGIN_ID, key,
                DEFAULT.readString(key, defaultValue), null);
        }

        private static void flush() {
            try {
                getInstanceScopePreferences().flush();
            } catch (BackingStoreException ex) {
                NodeLogger.getLogger(PythonPreferencesInitializer.class)
                    .error("Could not save Python preferences entry: " + ex.getMessage(), ex);
            }
        }

        private static final IEclipsePreferences getInstanceScopePreferences() {
            return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        }
    }

    private static final class DefaultScopePreferenceStorage implements PreferenceStorage {

        @Override
        public void writeBoolean(final String key, final boolean value) {
            getDefaultScopePreferences().putBoolean(key, value);
            flush();
        }

        @Override
        public boolean readBoolean(final String key, final boolean defaultValue) {
            return getDefaultScopePreferences().getBoolean(key, defaultValue);
        }

        @Override
        public void writeString(final String key, final String value) {
            getDefaultScopePreferences().put(key, value);
            flush();
        }

        @Override
        public String readString(final String key, final String defaultValue) {
            return getDefaultScopePreferences().get(key, defaultValue);
        }

        private static void flush() {
            try {
                getDefaultScopePreferences().flush();
            } catch (BackingStoreException ex) {
                NodeLogger.getLogger(PythonPreferencesInitializer.class)
                    .error("Could not save default Python preferences entry: " + ex.getMessage(), ex);
            }
        }

        private static IEclipsePreferences getDefaultScopePreferences() {
            return DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        }
    }
}
