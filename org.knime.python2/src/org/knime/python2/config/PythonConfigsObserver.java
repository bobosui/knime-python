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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Conda;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.CondaEnvironmentCreationStatus.CondaEnvironmentCreationStatusListener;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;

/**
 * Observes Python environment configurations and initiates installation tests as soon as relevant configuration entries
 * change. Clients can subscribe to changes of the status of such installation tests or can manually trigger such tests.
 * The observer updates all relevant installation status messages in {@link CondaEnvironmentConfig} and/or
 * {@link ManualEnvironmentConfig} as soon as a respective installation test finishes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class PythonConfigsObserver {

    private CopyOnWriteArrayList<PythonConfigsTestStatusListener> m_listeners = new CopyOnWriteArrayList<>();

    private PythonVersionConfig m_versionConfig;

    private PythonEnvironmentTypeConfig m_environmentTypeConfig;

    private CondaEnvironmentsConfig m_condaEnvironmentsConfig;

    private ManualEnvironmentsConfig m_manualEnvironmentsConfig;

    private SerializerConfig m_serializerConfig;

    /**
     * @param versionConfig Python version. Changes to the selected version are observed.
     * @param environmentTypeConfig Environment type. Changes to the selected environment type are observed.
     * @param condaEnvironmentsConfig Conda environment configuration. Changes to the conda directory path as well as to
     *            the Python 2 and Python 3 environments are observed.
     * @param python3EnvironmentCreationStatus
     * @param python2EnvironmentCreationStatus
     * @param manualEnvironmentsConfig Manual environment configuration. Changes to the Python 2 and Python 3 paths are
     *            observed.
     * @param serializerConfig Serializer configuration. Changes to the serializer are observed.
     */
    public PythonConfigsObserver(final PythonVersionConfig versionConfig,
        final PythonEnvironmentTypeConfig environmentTypeConfig, final CondaEnvironmentsConfig condaEnvironmentsConfig,
        final CondaEnvironmentCreationStatus python2EnvironmentCreationStatus,
        final CondaEnvironmentCreationStatus python3EnvironmentCreationStatus,
        final ManualEnvironmentsConfig manualEnvironmentsConfig, final SerializerConfig serializerConfig) {
        m_versionConfig = versionConfig;
        m_environmentTypeConfig = environmentTypeConfig;
        m_condaEnvironmentsConfig = condaEnvironmentsConfig;
        m_manualEnvironmentsConfig = manualEnvironmentsConfig;
        m_serializerConfig = serializerConfig;

        // Initialize view-model of default Python environment (since this was/is not persisted):

        updateDefaultPythonEnvironment();

        // Update default environment on version change.
        versionConfig.getPythonVersion().addChangeListener(e -> updateDefaultPythonEnvironment());

        // Test all environments of the respective type on environment type change:
        environmentTypeConfig.getEnvironmentType().addChangeListener(e -> {
            updateDefaultPythonEnvironment();
            testSelectedPythonEnvironmentType();
        });

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

        observeEnvironmentCreation(python2EnvironmentCreationStatus, false);
        observeEnvironmentCreation(python2EnvironmentCreationStatus, true);
    }

    private void updateDefaultPythonEnvironment() {
        final List<PythonEnvironmentConfig> notDefaultEnvironments = new ArrayList<>(4);
        Collections.addAll(notDefaultEnvironments, m_condaEnvironmentsConfig.getPython2Config(),
            m_condaEnvironmentsConfig.getPython3Config(), m_manualEnvironmentsConfig.getPython2Config(),
            m_manualEnvironmentsConfig.getPython3Config());

        final PythonEnvironmentsConfig environmentsOfCurrentType;
        final PythonEnvironmentType environmentType =
            PythonEnvironmentType.fromId(m_environmentTypeConfig.getEnvironmentType().getStringValue());
        if (PythonEnvironmentType.CONDA.equals(environmentType)) {
            environmentsOfCurrentType = m_condaEnvironmentsConfig;
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            environmentsOfCurrentType = m_manualEnvironmentsConfig;
        } else {
            throw new IllegalStateException("Selected environment type '" + environmentType.getName() + "' is neither "
                + "conda nor manual. This is an implementation error.");
        }
        final PythonEnvironmentConfig defaultEnvironment;
        final PythonVersion pythonVersion = PythonVersion.fromId(m_versionConfig.getPythonVersion().getStringValue());
        if (PythonVersion.PYTHON2.equals(pythonVersion)) {
            defaultEnvironment = environmentsOfCurrentType.getPython2Config();
        } else if (PythonVersion.PYTHON3.equals(pythonVersion)) {
            defaultEnvironment = environmentsOfCurrentType.getPython3Config();
        } else {
            throw new IllegalStateException("Selected default Python version is neither Python 2 nor Python 3. "
                + "This is an implementation error.");
        }
        notDefaultEnvironments.remove(defaultEnvironment);

        for (final PythonEnvironmentConfig notDefaultEnvironment : notDefaultEnvironments) {
            notDefaultEnvironment.getIsDefaultPythonEnvironment().setBooleanValue(false);
        }
        defaultEnvironment.getIsDefaultPythonEnvironment().setBooleanValue(true);
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
        new Thread(() -> {
            final Conda conda;
            try {
                conda = testCondaExecutable();
            } catch (final Exception ex) {
                return;
            }
            final List<String> availableEnvironments;
            try {
                availableEnvironments = getAvailableCondaEnvironments(conda);
            } catch (final Exception ex) {
                return;
            }

            try {
                setAvailableCondaEnvironments(false, availableEnvironments);
                testPythonEnvironment(true, false);
            } catch (Exception ex) {
                // Ignore, we still want to configure and test the second environment.
            }
            try {
                setAvailableCondaEnvironments(true, availableEnvironments);
                testPythonEnvironment(true, true);
            } catch (Exception ex) {
                // Ignore
            }
        }).start();
    }

    private Conda testCondaExecutable() throws Exception {
        final SettingsModelString condaInfoMessage = m_condaEnvironmentsConfig.getCondaInstallationInfo();
        final SettingsModelString condaErrorMessage = m_condaEnvironmentsConfig.getCondaInstallationError();
        try {
            condaInfoMessage.setStringValue("Testing Conda installation...");
            condaErrorMessage.setStringValue("");
            onCondaInstallationTestStarting();
            final Conda conda = new Conda(m_condaEnvironmentsConfig.getCondaDirectoryPath().getStringValue());
            final String condaVersion = conda.getVersionString();
            condaInfoMessage.setStringValue(condaVersion);
            condaErrorMessage.setStringValue("");
            onCondaInstallationTestFinished("");
            return conda;
        } catch (final Exception ex) {
            condaInfoMessage.setStringValue("");
            condaErrorMessage.setStringValue(ex.getMessage());
            clearAvailableCondaEnvironments(false);
            setCondaEnvironmentStatusMessages(false, "", "");
            clearAvailableCondaEnvironments(true);
            setCondaEnvironmentStatusMessages(true, "", "");
            onCondaInstallationTestFinished(ex.getMessage());
            throw ex;
        }
    }

    private List<String> getAvailableCondaEnvironments(final Conda conda) throws Exception {
        try {
            final String determiningEnvironmentsMessage = "Collecting available environments...";
            setCondaEnvironmentStatusMessages(false, determiningEnvironmentsMessage, "");
            setCondaEnvironmentStatusMessages(true, determiningEnvironmentsMessage, "");
            return conda.getEnvironments();
        } catch (final Exception ex) {
            m_condaEnvironmentsConfig.getCondaInstallationError().setStringValue(ex.getMessage());
            final String environmentsNotDetectedMessage = "Available environments could not be detected.";
            clearAvailableCondaEnvironments(false);
            setCondaEnvironmentStatusMessages(false, "", environmentsNotDetectedMessage);
            clearAvailableCondaEnvironments(true);
            setCondaEnvironmentStatusMessages(true, "", environmentsNotDetectedMessage);
            throw ex;
        }
    }

    private void clearAvailableCondaEnvironments(final boolean isPython3) {
        final CondaEnvironmentConfig condaConfig;
        final String placeholderEnvironmentName;
        if (isPython3) {
            condaConfig = m_condaEnvironmentsConfig.getPython3Config();
            placeholderEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON3_CONDA_ENV_NAME;
        } else {
            condaConfig = m_condaEnvironmentsConfig.getPython2Config();
            placeholderEnvironmentName = CondaEnvironmentsConfig.PLACEHOLDER_PYTHON2_CONDA_ENV_NAME;
        }
        condaConfig.getEnvironmentName().setStringValue(placeholderEnvironmentName);
        condaConfig.getAvailableEnvironmentNames().setStringArrayValue(new String[]{placeholderEnvironmentName});
    }

    private void setCondaEnvironmentStatusMessages(final boolean isPython3, final String infoMessage,
        final String errorMessage) {
        final CondaEnvironmentConfig condaEnvironmentConfig = isPython3 //
            ? m_condaEnvironmentsConfig.getPython3Config() //
            : m_condaEnvironmentsConfig.getPython2Config();
        condaEnvironmentConfig.getPythonInstallationInfo().setStringValue(infoMessage);
        condaEnvironmentConfig.getPythonInstallationError().setStringValue(errorMessage);
    }

    private void setAvailableCondaEnvironments(final boolean isPython3, List<String> availableEnvironments) {
        final CondaEnvironmentConfig condaConfig = isPython3 //
            ? m_condaEnvironmentsConfig.getPython3Config() //
            : m_condaEnvironmentsConfig.getPython2Config();
        if (availableEnvironments.isEmpty()) {
            availableEnvironments = Arrays.asList(isPython3 //
                ? CondaEnvironmentsConfig.PLACEHOLDER_PYTHON3_CONDA_ENV_NAME //
                : CondaEnvironmentsConfig.PLACEHOLDER_PYTHON2_CONDA_ENV_NAME);
        }
        condaConfig.getAvailableEnvironmentNames().setStringArrayValue(availableEnvironments.toArray(new String[0]));
        if (!availableEnvironments.contains(condaConfig.getEnvironmentName().getStringValue())) {
            condaConfig.getEnvironmentName().setStringValue(availableEnvironments.get(0));
        }
    }

    private void testPythonEnvironment(final boolean isConda, final boolean isPython3) {
        final PythonEnvironmentsConfig environmentsConfig;
        final PythonEnvironmentType environmentType;
        boolean isCondaPlaceholder = false;
        if (isConda) {
            if (isPlaceholderEnvironmentSelected(isPython3)) {
                // We don't want to test the placeholder but just clear the installation status messages and return.
                isCondaPlaceholder = true;
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
        if (isCondaPlaceholder) {
            infoMessage.setStringValue("");
            errorMessage.setStringValue(
                "No environment available. Please create a new one to be able to use " + pythonVersion.getName() + ".");
            return;
        }
        final Collection<PythonModuleSpec> requiredSerializerModules = SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules();
        infoMessage.setStringValue("Testing " + pythonVersion.getName() + " environment...");
        errorMessage.setStringValue("");
        onEnvironmentInstallationTestStarting(environmentType, pythonVersion);
        final PythonCommand pythonCommand = environmentConfig.getPythonCommand();
        new Thread(() -> {
            final PythonKernelTestResult testResult = isPython3 //
                ? PythonKernelTester.testPython3Installation(pythonCommand, requiredSerializerModules, true) //
                : PythonKernelTester.testPython2Installation(pythonCommand, requiredSerializerModules, true);
            infoMessage.setStringValue(testResult.getVersion());
            errorMessage.setStringValue(testResult.getErrorLog());
            onEnvironmentInstallationTestFinished(environmentType, pythonVersion, testResult);
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

    private void observeEnvironmentCreation(final CondaEnvironmentCreationStatus creationStatus,
        final boolean isPython3) {
        creationStatus.addEnvironmentCreationStatusListener(new CondaEnvironmentCreationStatusListener() {

            @Override
            public void condaEnvironmentCreationStarting() {
                // no-op
            }

            @Override
            public void condaEnvironmentCreationFinished(final String environmentName) {
                // TODO: refresh available environmetns, select new environment
            }

            @Override
            public void condaEnvironmentCreationFailed(final String errorMessage) {
                // no-op
            }

            @Override
            public void condaEnvironmentCreationCanceled() {
                // no-op
            }
        });
    }

    private synchronized void onCondaInstallationTestStarting() {
        for (final PythonConfigsTestStatusListener listener : m_listeners) {
            listener.condaInstallationTestStarting();
        }
    }

    private synchronized void onCondaInstallationTestFinished(final String errorMessage) {
        for (final PythonConfigsTestStatusListener listener : m_listeners) {
            listener.condaInstallationTestFinished(errorMessage);
        }
    }

    private synchronized void onEnvironmentInstallationTestStarting(final PythonEnvironmentType environmentType,
        final PythonVersion pythonVersion) {
        for (final PythonConfigsTestStatusListener listener : m_listeners) {
            listener.environmentInstallationTestStarting(environmentType, pythonVersion);
        }
    }

    private synchronized void onEnvironmentInstallationTestFinished(final PythonEnvironmentType environmentType,
        final PythonVersion pythonVersion, final PythonKernelTestResult testResult) {
        for (final PythonConfigsTestStatusListener listener : m_listeners) {
            listener.environmentInstallationTestFinished(environmentType, pythonVersion, testResult);
        }
    }

    /**
     * @param listener A listener which will be notified about changes in the status of any installation test initiated
     *            by this instance.
     */
    public void addConfigsTestStatusListener(final PythonConfigsTestStatusListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * @param listener The listener to remove.
     * @return {@code true} if the listener was present before removal.
     */
    public boolean removeConfigsTestStatusListener(final PythonConfigsTestStatusListener listener) {
        return m_listeners.remove(listener);
    }

    /**
     * Listener which will be notified about changes in the status of installation tests initiated by the enclosing
     * class.
     */
    public static interface PythonConfigsTestStatusListener {

        /**
         * Called synchronously.
         */
        void condaInstallationTestStarting();

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         *
         * @param errorMessage Error messages that occurred during the installation test. Empty if the installation test
         *            was successful, i.e., conda is properly installed.
         */
        void condaInstallationTestFinished(String errorMessage);

        /**
         * Called asynchronously.
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
