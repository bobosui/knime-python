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
 *   Feb 14, 2019 (marcel): created
 */
package org.knime.python2.prefs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class InstallationStatusDisplayPanel extends Composite {

    private final Label m_info;

    private final Label m_error;

    public InstallationStatusDisplayPanel(final SettingsModelString infoMessageModel,
        final SettingsModelString errorMessageModel, final Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout());

        // Info label:
        m_info = new Label(this, SWT.NONE);
        setInfoMessage(infoMessageModel.getStringValue());
        m_info.setLayoutData(new GridData());

        // Error label:
        m_error = new Label(this, SWT.NONE);
        final Color red = new Color(parent.getDisplay(), 255, 0, 0);
        m_error.setForeground(red);
        m_error.addDisposeListener(e -> red.dispose());
        setErrorMessage(errorMessageModel.getStringValue());
        m_error.setLayoutData(new GridData());

        // Hooks:

        infoMessageModel.addChangeListener(e -> setInfoMessage(infoMessageModel.getStringValue()));
        errorMessageModel.addChangeListener(e -> setErrorMessage(errorMessageModel.getStringValue()));
    }

    private void setInfoMessage(final String info) {
        if (info == null) {
            m_info.setText("");
        } else {
            m_info.setText(info);
        }
        layout();
    }

    private void setErrorMessage(final String error) {
        if (error != null) {
            m_error.setText(error);
        } else {
            m_error.setText("");
        }
        layout();
    }
}