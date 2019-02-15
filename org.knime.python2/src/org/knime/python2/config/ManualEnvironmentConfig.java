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
 *   Jan 24, 2019 (marcel): created
 */
package org.knime.python2.config;

import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class ManualEnvironmentConfig extends AbstractEnvironmentConfig {

    /**
     * Configuration key for the path to the Python 2 executable ("environment").
     */
    public static final String CFG_KEY_PYTHON2_PATH = "python2Path";

    /**
     * Configuration key for the path to the Python 3 executable ("environment").
     */
    public static final String CFG_KEY_PYTHON3_PATH = "python3Path";

    /**
     * Use the command 'python' without a specified location by default.
     */
    public static final String DEFAULT_PYTHON2_PATH = "python";

    /**
     * Use the command 'python3' without a specified location by default.
     */
    public static final String DEFAULT_PYTHON3_PATH = "python3";

    private final SettingsModelString m_python2Path =
        new SettingsModelString(CFG_KEY_PYTHON2_PATH, DEFAULT_PYTHON2_PATH);

    private final SettingsModelString m_python3Path =
        new SettingsModelString(CFG_KEY_PYTHON3_PATH, DEFAULT_PYTHON3_PATH);

    /**
     * @return The path to the Python 2 executable.
     */
    public SettingsModelString getPython2Path() {
        return m_python2Path;
    }

    /**
     * @return The path to the Python 3 executable.
     */
    public SettingsModelString getPython3Path() {
        return m_python3Path;
    }
}
