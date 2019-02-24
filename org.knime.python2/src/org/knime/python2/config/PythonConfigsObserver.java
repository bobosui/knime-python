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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Conda;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.PythonVersion;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;

/**
 * Observes Python environment configurations and initiates installation tests as soon as relevant configuration entries
 * change. Clients can subscribe to changes to the status of such installation tests or can manually trigger such tests.
 * The observer updates all relevant installation status messages in {@link CondaEnvironmentConfig} and/or
 * {@link ManualEnvironmentConfig} as soon as a respective installation test finishes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class PythonConfigsObserver {

    private CopyOnWriteArrayList<PythonEnvironmentConfigTestStatusListener> m_listeners = new CopyOnWriteArrayList<>();

    private PythonEnvironmentTypeConfig m_environmentTypeConfig;

    private CondaEnvironmentsConfig m_condaEnvironmentsConfig;

    private ManualEnvironmentsConfig m_manualEnvironmentsConfig;

    private SerializerConfig m_serializerConfig;

    /**
     * @param environmentTypeConfig Environment type. Changes to the selected environment type are observed.
     * @param condaEnvironmentsConfig Conda environment configuration. Changes to the conda directory path as well as to
     *            the Python 2 and Python 3 environments are observed.
     * @param manualEnvironmentsConfig Manual environment configuration. Changes to the Python 2 and Python 3 paths are
     *            observed.
     * @param serializerConfig Serializer configuration. Changes to the serializer are observed.
     */
    public PythonConfigsObserver(final PythonEnvironmentTypeConfig environmentTypeConfig,
        final CondaEnvironmentsConfig condaEnvironmentsConfig, final ManualEnvironmentsConfig manualEnvironmentsConfig,
        final SerializerConfig serializerConfig) {
        m_environmentTypeConfig = environmentTypeConfig;
        m_condaEnvironmentsConfig = condaEnvironmentsConfig;
        m_manualEnvironmentsConfig = manualEnvironmentsConfig;
        m_serializerConfig = serializerConfig;

        // Test all environments of the respective type on environment type change:
        environmentTypeConfig.getEnvironmentType().addChangeListener(e -> testSelectedPythonEnvironmentType());

        // Refresh and test entire conda config on conda directory change.
        condaEnvironmentsConfig.getCondaDirectoryPath().addChangeListener(e -> refreshAndTestCondaConfig());

        // Test conda environments on change:
        condaEnvironmentsConfig.getPython2Config().getEnvironmentName()
            .addChangeListener(e -> testPythonEnvironment(true, false));
        condaEnvironmentsConfig.getPython3Config().getEnvironmentName()
            .addChangeListener(e -> testPythonEnvironment(true, true));

        // Test manual environments on change:
        manualEnvironmentsConfig.getPython2Config().getExecutablePath()
            .addChangeListener(e -> testPythonEnvironment(false, false));
        manualEnvironmentsConfig.getPython3Config().getExecutablePath()
            .addChangeListener(e -> testPythonEnvironment(false, true));

        // Test required external modules of serializer on change:
        serializerConfig.getSerializer().addChangeListener(e -> testSelectedPythonEnvironmentType());
    }

    /**
     * Initiates installation tests for all environments of the currently selected {@link PythonEnvironmentType}.
     * Depending on the selected type, the status of each of these tests is published to all installation status models
     * in either the observed {@link CondaEnvironmentConfig} or the observed {@link ManualEnvironmentConfig}.
     */
    public void testSelectedPythonEnvironmentType() {
        final PythonEnvironmentType environmentType =
            PythonEnvironmentType.fromId(m_environmentTypeConfig.getEnvironmentType().getStringValue());
        if (PythonEnvironmentType.CONDA.equals(environmentType)) {
            refreshAndTestCondaConfig();
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            testPythonEnvironment(false, false);
            testPythonEnvironment(false, true);
        } else {
            throw new IllegalStateException("Selected environment type '" + environmentType.getName() + "' is neither "
                + "conda nor manual. This is an implementation error.");
        }
    }

    private void refreshAndTestCondaConfig() {
        try {
            testCondaExecutable();
        } catch (final Exception ex) {
            clearAvailableCondaEnvironments(false, true);
            clearAvailableCondaEnvironments(true, true);
            return;
        }

        try {
            refreshAvailableCondaEnvironment(false);
            testPythonEnvironment(true, false); // TODO: We don't want to clear the list of available environments if the test fails!
        } catch (Exception ex) {
            clearAvailableCondaEnvironments(false, false);
        }
        try {
            refreshAvailableCondaEnvironment(true);
            testPythonEnvironment(true, true);
        } catch (Exception ex) {
            clearAvailableCondaEnvironments(true, false);
        }
    }

    private void testCondaExecutable() throws Exception {
        final SettingsModelString infoMessage = m_condaEnvironmentsConfig.getCondaInstallationInfo();
        final SettingsModelString errorMessage = m_condaEnvironmentsConfig.getCondaInstallationError();
        try {
            infoMessage.setStringValue("Testing Conda installation...");
            errorMessage.setStringValue("");
            onCondaInstallationTestStarting();
            final String condaVersion =
                new Conda(m_condaEnvironmentsConfig.getCondaDirectoryPath().getStringValue()).getVersionString();
            infoMessage.setStringValue(condaVersion);
            errorMessage.setStringValue("");
            onCondaInstallationTestFinished("");
        } catch (final Exception ex) {
            infoMessage.setStringValue("");
            errorMessage.setStringValue(ex.getMessage());
            onCondaInstallationTestFinished(ex.getMessage());
            throw ex;
        }
    }

    private void refreshAvailableCondaEnvironment(final boolean isPython3) throws Exception {
        final CondaEnvironmentConfig condaConfig = isPython3 //
            ? m_condaEnvironmentsConfig.getPython3Config() //
            : m_condaEnvironmentsConfig.getPython2Config();
        try {
            final Conda conda = new Conda(m_condaEnvironmentsConfig.getCondaDirectoryPath().getStringValue());
            List<String> environments = conda.getEnvironments();
            if (environments.isEmpty()) {
                environments = Arrays.asList(isPython3 //
                    ? CondaEnvironmentsConfig.PLACEHOLDER_PYTHON3_CONDA_ENV_NAME //
                    : CondaEnvironmentsConfig.PLACEHOLDER_PYTHON2_CONDA_ENV_NAME);
            }
            condaConfig.getAvailableEnvironmentNames().setStringArrayValue(environments.toArray(new String[0]));
            if (!environments.contains(condaConfig.getEnvironmentName().getStringValue())) {
                condaConfig.getEnvironmentName().setStringValue(environments.get(0));
            }
        } catch (Exception ex) {
            condaConfig.getPythonInstallationInfo().setStringValue("");
            condaConfig.getPythonInstallationError().setStringValue(ex.getMessage());
            throw ex;
        }
    }

    private void clearAvailableCondaEnvironments(final boolean isPython3, final boolean clearInstallationMessages) {
        final CondaEnvironmentConfig condaConfig;
        final String placeholderEnvironmentName;
        if (isPython3) {
            condaConfig = m_condaEnvironmentsConfig.getPython3Config();
            placeholderEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON3_CONDA_ENV_NAME;
        } else {
            condaConfig = m_condaEnvironmentsConfig.getPython2Config();
            placeholderEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON2_CONDA_ENV_NAME;
        }
        condaConfig.getAvailableEnvironmentNames().setStringArrayValue(new String[]{placeholderEnvironmentName});
        condaConfig.getEnvironmentName().setStringValue(placeholderEnvironmentName);
        if (clearInstallationMessages) {
            condaConfig.getPythonInstallationInfo().setStringValue("");
            condaConfig.getPythonInstallationError().setStringValue("");
        }
    }

    private void testPythonEnvironment(final boolean isConda, final boolean isPython3) {
        final PythonEnvironmentsConfig environmentsConfig;
        final PythonEnvironmentType environmentType;
        boolean doNotTest = false;
        if (isConda) {
            if (isPlaceholderEnvironmentSelected(isPython3)) {
                // We don't want to test the placeholder but just clear the installation status messages and return.
                doNotTest = true;
            }
            environmentsConfig = m_condaEnvironmentsConfig;
            environmentType = PythonEnvironmentType.CONDA;
        } else {
            environmentsConfig = m_manualEnvironmentsConfig;
            environmentType = PythonEnvironmentType.MANUAL;
        }
        final PythonEnvironmentConfig environmentConfig;
        final PythonVersion pythonVersion;
        if (isPython3) {
            environmentConfig = environmentsConfig.getPython3Config();
            pythonVersion = PythonVersion.PYTHON3;
        } else {
            environmentConfig = environmentsConfig.getPython2Config();
            pythonVersion = PythonVersion.PYTHON2;
        }
        final SettingsModelString infoMessage = environmentConfig.getPythonInstallationInfo();
        final SettingsModelString errorMessage = environmentConfig.getPythonInstallationError();
        if (doNotTest) {
            infoMessage.setStringValue("");
            errorMessage.setStringValue("");
            return;
        }
        final Collection<PythonModuleSpec> requiredSerializerModules = SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules();
        infoMessage.setStringValue("Testing Python environment...");
        errorMessage.setStringValue("");
        onInstallationTestStarting(environmentType, pythonVersion);
        final PythonCommand pythonCommand = environmentConfig.getPythonCommand();
        new Thread(() -> {
            final PythonKernelTestResult testResult = isPython3 //
                ? PythonKernelTester.testPython3Installation(pythonCommand, requiredSerializerModules, true) //
                : PythonKernelTester.testPython2Installation(pythonCommand, requiredSerializerModules, true);
            infoMessage.setStringValue(testResult.getVersion());
            errorMessage.setStringValue(testResult.getErrorLog());
            onInstallationTestFinished(environmentType, pythonVersion, testResult);
        }).start();
    }

    private boolean isPlaceholderEnvironmentSelected(final boolean isPython3) {
        final SettingsModelString condaEnvironmentName;
        final String dummyCondaEnvironmentName;
        if (isPython3) {
            condaEnvironmentName = m_condaEnvironmentsConfig.getPython3Config().getEnvironmentName();
            dummyCondaEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON3_CONDA_ENV_NAME;
        } else {
            condaEnvironmentName = m_condaEnvironmentsConfig.getPython2Config().getEnvironmentName();
            dummyCondaEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON2_CONDA_ENV_NAME;
        }
        return dummyCondaEnvironmentName.equals(condaEnvironmentName.getStringValue());
    }

    private synchronized void onCondaInstallationTestStarting() {
        for (final PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.condaInstallationTestStarting();
        }
    }

    private synchronized void onCondaInstallationTestFinished(final String errorMessage) {
        for (final PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.condaInstallationTestFinished(errorMessage);
        }
    }

    private synchronized void onInstallationTestStarting(final PythonEnvironmentType environmentType,
        final PythonVersion pythonVersion) {
        for (final PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.environmentInstallationTestStarting(environmentType, pythonVersion);
        }
    }

    private synchronized void onInstallationTestFinished(final PythonEnvironmentType environmentType,
        final PythonVersion pythonVersion, final PythonKernelTestResult testResult) {
        for (final PythonEnvironmentConfigTestStatusListener listener : m_listeners) {
            listener.environmentInstallationTestFinished(environmentType, pythonVersion, testResult);
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
         */
        void condaInstallationTestStarting();

        /**
         * Called synchronously.
         *
         * @param errorMessage Error messages that occurred during the installation test. Empty if the installation test
         *            was successful, i.e., conda is properly installed.
         */
        void condaInstallationTestFinished(String errorMessage);

        /**
         * Called synchronously.
         *
         * @param environmentType The environment type of the environment whose installation test is about to start.
         * @param pythonVersion The Python version of the environment.
         */
        void environmentInstallationTestStarting(PythonEnvironmentType environmentType, PythonVersion pythonVersion);

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         *
         * @param environmentType The environment type of the environment whose installation test has finished.
         * @param pythonVersion The Python version of the environment.
         * @param testResult The result of the installation test.
         */
        void environmentInstallationTestFinished(PythonEnvironmentType environmentType, PythonVersion pythonVersion,
            PythonKernelTestResult testResult);
    }
}
