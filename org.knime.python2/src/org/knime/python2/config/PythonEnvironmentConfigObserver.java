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
import org.knime.python2.Conda;
import org.knime.python2.DefaultPythonCommand;
import org.knime.python2.PythonCommand;
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

    private EnvironmentTypeConfig m_environmentTypeConfig;

    private CondaEnvironmentConfig m_condaEnvironmentConfig;

    private ManualEnvironmentConfig m_manualEnvironmentConfig;

    private SerializerConfig m_serializerConfig;

    /**
     * @param environmentTypeConfig Environment type. Changes to the selected environment type are observed.
     * @param condaEnvironmentConfig Conda environment configuration. Changes to the conda executable path as well as to
     *            the Python 2 and Python 3 environments are observed.
     * @param manualEnvironmentConfig Manual environment configuration. Changes to the Python 2 and Python 3 paths are
     *            observed.
     * @param serializerConfig Serializer configuration. Changes to the serializer are observed.
     */
    public PythonEnvironmentConfigObserver(final EnvironmentTypeConfig environmentTypeConfig,
        final CondaEnvironmentConfig condaEnvironmentConfig, final ManualEnvironmentConfig manualEnvironmentConfig,
        final SerializerConfig serializerConfig) {
        m_environmentTypeConfig = environmentTypeConfig;
        m_condaEnvironmentConfig = condaEnvironmentConfig;
        m_manualEnvironmentConfig = manualEnvironmentConfig;
        m_serializerConfig = serializerConfig;

        // Test all environments of the respective type on environment type change:
        environmentTypeConfig.getEnvironmentType().addChangeListener(e -> testSelectedPythonEnvironmentType());

        // Test conda environments on change:

        // TODO: Also refresh list of available conda environments if conda executable changes.

        final SettingsModelString python2CondaEnv = condaEnvironmentConfig.getPython2Environment();
        python2CondaEnv.addChangeListener(
            e -> testPythonEnvironment(python2CondaEnv.getKey(), python2CondaEnv.getStringValue(), false, true));
        final SettingsModelString python3CondaEnv = condaEnvironmentConfig.getPython3Environment();
        python3CondaEnv.addChangeListener(
            e -> testPythonEnvironment(python3CondaEnv.getKey(), python3CondaEnv.getStringValue(), true, true));

        // Test manual environments on change:
        final SettingsModelString python2ManualEnv = manualEnvironmentConfig.getPython2Path();
        python2ManualEnv.addChangeListener(
            e -> testPythonEnvironment(python2ManualEnv.getKey(), python2ManualEnv.getStringValue(), false, false));
        final SettingsModelString python3ManualEnv = manualEnvironmentConfig.getPython3Path();
        python3ManualEnv.addChangeListener(
            e -> testPythonEnvironment(python3ManualEnv.getKey(), python3ManualEnv.getStringValue(), true, false));

        // Test required external modules of serializer on change:
        serializerConfig.getSerializer().addChangeListener(e -> testSelectedPythonEnvironmentType());
    }

    /**
     * Initiates installation tests for all environments of the currently selected {@link PythonEnvironmentType}. The
     * status of each of these tests is published to all listeners registered via
     * {@link #addConfigTestStatusListener(PythonEnvironmentConfigTestStatusListener)}. Also, all installation status
     * messages in either the observed {@link CondaEnvironmentConfig} or the observed {@link ManualEnvironmentConfig}
     * (depending on the selected type) are updated.
     */
    public void testSelectedPythonEnvironmentType() {
        final PythonEnvironmentType environmentType =
            PythonEnvironmentType.fromId(m_environmentTypeConfig.getEnvironmentType().getStringValue());
        if (PythonEnvironmentType.CONDA.equals(environmentType)) {
            final SettingsModelString python2CondaEnv = m_condaEnvironmentConfig.getPython2Environment();
            testPythonEnvironment(python2CondaEnv.getKey(), python2CondaEnv.getStringValue(), false, true);
            final SettingsModelString python3CondaEnv = m_condaEnvironmentConfig.getPython3Environment();
            testPythonEnvironment(python3CondaEnv.getKey(), python3CondaEnv.getStringValue(), true, true);
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            final SettingsModelString python2ManualEnv = m_manualEnvironmentConfig.getPython2Path();
            testPythonEnvironment(python2ManualEnv.getKey(), python2ManualEnv.getStringValue(), false, false);
            final SettingsModelString python3ManualEnv = m_manualEnvironmentConfig.getPython3Path();
            testPythonEnvironment(python3ManualEnv.getKey(), python3ManualEnv.getStringValue(), true, false);
        } else {
            throw new IllegalStateException("Selected environment type '" + environmentType.getName() + "' is neither "
                + "conda nor manual. This is an implementation error.");
        }
    }

    private void testPythonEnvironment(final String environmentKey, final String environmentNameOrPath,
        final boolean isPython3, final boolean isConda) {
        onInstallationTestStarting(environmentKey);
        final PythonCommand pythonCommand;
        if (isConda) {
            // We only have the environment name and need to resolve it to an executable command.
            pythonCommand = Conda.getPythonCommand(m_condaEnvironmentConfig.getCondaExecutablePath().getStringValue(),
                environmentNameOrPath);
        } else {
            pythonCommand = new DefaultPythonCommand(environmentNameOrPath);
        }
        final Collection<PythonModuleSpec> requiredSerializerModules = SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules();
        new Thread(() -> {
            final PythonKernelTestResult testResult = isPython3 //
                ? PythonKernelTester.testPython3Installation(pythonCommand, requiredSerializerModules, true) //
                : PythonKernelTester.testPython2Installation(pythonCommand, requiredSerializerModules, true);
            onInstallationTestFinished(environmentKey, testResult);
        }).start();
    }

//    @Override
//    public void installationTestStarting(final String environmentKey) {
//        final PythonPathEditor pythonPathEditorForEnvironmentKey =
//            getEditorForEnvironmentKey(m_manualEnvironmentPanel, environmentKey);
//        pythonPathEditorForEnvironmentKey.setInfo("Testing Python installation...");
//        pythonPathEditorForEnvironmentKey.setError(null);
//    }
//
//    @Override
//    public void installationTestFinished(final String environmentKey, final PythonKernelTestResult testResult) {
//        final PythonPathEditor pythonPathEditorForEnvironmentKey =
//            getEditorForEnvironmentKey(m_manualEnvironmentPanel, environmentKey);
//        m_parentDisplay.asyncExec(() -> {
//            if (!getControl().isDisposed()) {
//                pythonPathEditorForEnvironmentKey.setInfo(testResult.getVersion());
//                pythonPathEditorForEnvironmentKey.setError(testResult.getErrorLog());
//                m_container.layout();
//                m_containerScrolledView.setMinSize(m_container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//            }
//        });
//    }

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
