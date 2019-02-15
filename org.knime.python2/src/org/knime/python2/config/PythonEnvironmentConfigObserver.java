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
package org.knime.python2.config;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;

/**
 * Observes Python environment configurations and initiates installation tests as soon as relevant configuration entries
 * change. Clients can subscribe to changes to the status of such installation tests or can manually trigger such tests.
 * The observer updates all relevant installation status messages in {@link CondaEnvironmentConfig} and/or
 * {@link ManualEnvironmentConfig} as soon as an installation test finishes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class PythonEnvironmentConfigObserver {

    private CopyOnWriteArrayList<PythonEnvironmentConfigTestStatusListener> m_listeners = new CopyOnWriteArrayList<>();

    private ManualEnvironmentConfig m_manualEnvironmentConfig;

    private SerializerConfig m_serializerConfig;

    /**
     * @param condaEnvironmentConfig Conda environment configuration. Changes to the conda executable path as well as to
     *            the Python 2 and Python 3 environments are observed.
     * @param manualEnvironmentConfig Manual environment configuration. Changes to the Python 2 and Python 3 paths are
     *            observed.
     * @param serializerConfig Serializer configuration. Changes to the serializer are observed.
     */
    public PythonEnvironmentConfigObserver(final CondaEnvironmentConfig condaEnvironmentConfig,
        final ManualEnvironmentConfig manualEnvironmentConfig, final SerializerConfig serializerConfig) {
        m_manualEnvironmentConfig = manualEnvironmentConfig;
        m_serializerConfig = serializerConfig;
        // Test Python environments on change.
        final SettingsModelString python2Path = manualEnvironmentConfig.getPython2Path();
        python2Path
            .addChangeListener(e -> testPythonEnvironment(python2Path.getKey(), python2Path.getStringValue(), false));
        final SettingsModelString python3Path = manualEnvironmentConfig.getPython3Path();
        python3Path
            .addChangeListener(e -> testPythonEnvironment(python3Path.getKey(), python3Path.getStringValue(), true));
        // Test required external modules of serializer on change.
        serializerConfig.getSerializer().addChangeListener(e -> testAllPythonEnvironments());
    }

    /**
     * Initiates installation tests for all environments that are observed by this instance. The status of each of these
     * tests is published to all listeners registered via
     * {@link #addConfigTestStatusListener(PythonEnvironmentConfigTestStatusListener)}. Also, all installation status
     * messages in {@link CondaEnvironmentConfig} and {@link ManualEnvironmentConfig} are updated.
     */
    public void testAllPythonEnvironments() {
        final SettingsModelString python2Path = m_manualEnvironmentConfig.getPython2Path();
        testPythonEnvironment(python2Path.getKey(), python2Path.getStringValue(), false);
        final SettingsModelString python3Path = m_manualEnvironmentConfig.getPython3Path();
        testPythonEnvironment(python3Path.getKey(), python3Path.getStringValue(), true);
    }

    private void testPythonEnvironment(final String environmentKey, final String environmentCommand,
        final boolean isPython3) {
        onInstallationTestStarting(environmentKey);
        final Collection<PythonModuleSpec> requiredSerializerModules = SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules();
        new Thread(() -> {
            final PythonKernelTestResult testResult = isPython3 //
                ? PythonKernelTester.testPython3Installation(environmentCommand, requiredSerializerModules, true) //
                : PythonKernelTester.testPython2Installation(environmentCommand, requiredSerializerModules, true);
            onInstallationTestFinished(environmentKey, testResult);
        }).start();
    }

    private synchronized void onInstallationTestStarting(final String environmentKey) {
        for (PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.installationTestStarting(environmentKey);
        }
    }

    private synchronized void onInstallationTestFinished(final String environmentKey,
        final PythonKernelTestResult testResult) {
        for (PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.installationTestFinished(environmentKey, testResult);
        }
    }

    /**
     * @param listener A listener which will be notified about changes in the status of any installation test initiated
     *            by this instance.
     */
    public void addConfigTestStatusListener(final PythonEnvironmentConfigTestStatusListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * @param listener The listener to remove.
     * @return {@code true} if the listener was present before removal.
     */
    public boolean removeConfigTestStatusListener(final PythonEnvironmentConfigTestStatusListener listener) {
        return m_listeners.remove(listener);
    }

    /**
     * Listener which will be notified about changes in the status of installation tests initiated by the enclosing
     * class.
     */
    public static interface PythonEnvironmentConfigTestStatusListener {

        /**
         * Called synchronously.
         *
         * @param environmentKey The (settings) key of the environment for which an installation test will be initiated.
         */
        void installationTestStarting(String environmentKey);

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         *
         * @param environmentKey The (settings) key of the environment for which an installation test has been finished.
         * @param testResult The result of the installation test.
         */
        void installationTestFinished(String environmentKey, PythonKernelTestResult testResult);
    }
}
