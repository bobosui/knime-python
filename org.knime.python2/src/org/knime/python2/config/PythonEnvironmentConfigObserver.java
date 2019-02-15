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

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;

/**
 * Observes Python environment configurations and initiates installation tests as soon as relevant configuration entries
 * change. Updates all relevant installation status messages in {@link CondaEnvironmentConfig} and/or
 * {@link ManualEnvironmentConfig} as soon as a respective installation test finishes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class PythonEnvironmentConfigObserver {

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

        condaEnvironmentConfig.getPython2EnvironmentName().addChangeListener(e -> testPythonEnvironment(true, false));
        condaEnvironmentConfig.getPython3EnvironmentName().addChangeListener(e -> testPythonEnvironment(true, true));

        // Test manual environments on change:
        manualEnvironmentConfig.getPython2Path().addChangeListener(e -> testPythonEnvironment(false, false));
        manualEnvironmentConfig.getPython3Path().addChangeListener(e -> testPythonEnvironment(false, true));

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
            testPythonEnvironment(true, false);
            testPythonEnvironment(true, true);
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            testPythonEnvironment(false, false);
            testPythonEnvironment(false, true);
        } else {
            throw new IllegalStateException("Selected environment type '" + environmentType.getName() + "' is neither "
                + "conda nor manual. This is an implementation error.");
        }
    }

    private void testPythonEnvironment(final boolean isConda, final boolean isPython3) {
        final PythonEnvironmentConfig environmentConfig = isConda //
            ? m_condaEnvironmentConfig //
            : m_manualEnvironmentConfig;
        final PythonCommand pythonCommand;
        final SettingsModelString installationInfo;
        final SettingsModelString installationError;
        if (isPython3) {
            pythonCommand = environmentConfig.getPython3Command();
            installationInfo = environmentConfig.getPython3InstallationInfo();
            installationError = environmentConfig.getPython3InstallationError();
        } else {
            pythonCommand = environmentConfig.getPython2Command();
            installationInfo = environmentConfig.getPython2InstallationInfo();
            installationError = environmentConfig.getPython2InstallationError();
        }
        final Collection<PythonModuleSpec> requiredSerializerModules = SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules();

        installationInfo.setStringValue("Testing Python installation...");
        installationError.setStringValue("");
        new Thread(() -> {
            final PythonKernelTestResult testResult = isPython3 //
                ? PythonKernelTester.testPython3Installation(pythonCommand, requiredSerializerModules, true) //
                : PythonKernelTester.testPython2Installation(pythonCommand, requiredSerializerModules, true);
            installationInfo.setStringValue(testResult.getVersion());
            installationError.setStringValue(testResult.getErrorLog());
        }).start();
    }
}
