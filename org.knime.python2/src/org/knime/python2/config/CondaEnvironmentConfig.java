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
 *   Feb 3, 2019 (marcel): created
 */
package org.knime.python2.config;

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.python2.Conda;
import org.knime.python2.PythonCommand;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class CondaEnvironmentConfig extends AbstractPythonEnvironmentConfig {

    /**
     * Configuration key for the path to the conda executable.
     */
    public static final String CFG_KEY_CONDA_EXECUTABLE_PATH = "condaExecutable";

    /**
     * Configuration key for the Python 2 conda environment.
     */
    public static final String CFG_KEY_PYTHON2_CONDA_ENV_NAME = "python2CondaEnvironmentName";

    /**
     * Configuration key for the Python 3 conda environment.
     */
    public static final String CFG_KEY_PYTHON3_CONDA_ENV_NAME = "python3CondaEnvironmentName";

    /**
     * Use command 'conda' without a specified location by default.
     */
    public static final String DEFAULT_CONDA_EXECUTABLE_PATH = "conda";

    /**
     * Use no environment by default.
     */
    public static final String PLACEHOLDER_PYTHON2_CONDA_ENV_NAME = "<no environment>";

    /**
     * Use no environment by default.
     */
    public static final String PLACEHOLDER_PYTHON3_CONDA_ENV_NAME = "<no environment>";

    private final SettingsModelString m_condaExecutable =
        new SettingsModelString(CFG_KEY_CONDA_EXECUTABLE_PATH, DEFAULT_CONDA_EXECUTABLE_PATH);

    private final SettingsModelString m_python2Environment =
        new SettingsModelString(CFG_KEY_PYTHON2_CONDA_ENV_NAME, PLACEHOLDER_PYTHON2_CONDA_ENV_NAME);

    private final SettingsModelString m_python3Environment =
        new SettingsModelString(CFG_KEY_PYTHON3_CONDA_ENV_NAME, PLACEHOLDER_PYTHON3_CONDA_ENV_NAME);

    // Not meant for saving/loading. We just want observable values here to communicate with the view:

    private static final String DUMMY_CFG_KEY = "dummy";

    private final SettingsModelString m_condaInstallationInfo = new SettingsModelString(DUMMY_CFG_KEY, "");

    private final SettingsModelString m_condaInstallationError = new SettingsModelString(DUMMY_CFG_KEY, "");

    private final SettingsModelStringArray m_python2AvailableEnvironments =
        new SettingsModelStringArray(DUMMY_CFG_KEY, new String[]{PLACEHOLDER_PYTHON2_CONDA_ENV_NAME});

    private final SettingsModelStringArray m_python3AvailableEnvironments =
        new SettingsModelStringArray(DUMMY_CFG_KEY, new String[]{PLACEHOLDER_PYTHON3_CONDA_ENV_NAME});

    /**
     * @return The path to the conda executable.
     */
    public SettingsModelString getCondaExecutablePath() {
        return m_condaExecutable;
    }

    /**
     * @return The installation status message of the conda executable. Not meant for saving/loading.
     */
    public SettingsModelString getCondaInstallationInfo() {
        return m_condaInstallationInfo;
    }

    /**
     * @return The installation error message of the conda executable. Not meant for saving/loading.
     */
    public SettingsModelString getCondaInstallationError() {
        return m_condaInstallationError;
    }

    /**
     * @return The name of the Python 2 conda environment.
     */
    public SettingsModelString getPython2EnvironmentName() {
        return m_python2Environment;
    }

    /**
     * @return The list of currently available Python 2 conda environments. Not meant for saving/loading.
     */
    public SettingsModelStringArray getPython2AvailableEnvironments() {
        return m_python2AvailableEnvironments;
    }

    @Override
    public PythonCommand getPython2Command() {
        return Conda.createPythonCommand(m_condaExecutable.getStringValue(), m_python2Environment.getStringValue());
    }

    /**
     * @return The name of the Python 3 conda environment.
     */
    public SettingsModelString getPython3EnvironmentName() {
        return m_python3Environment;
    }

    /**
     * @return The list of currently available Python 3 conda environments. Not meant for saving/loading.
     */
    public SettingsModelStringArray getPython3AvailableEnvironments() {
        return m_python3AvailableEnvironments;
    }

    @Override
    public PythonCommand getPython3Command() {
        return Conda.createPythonCommand(m_condaExecutable.getStringValue(), m_python3Environment.getStringValue());
    }
}
