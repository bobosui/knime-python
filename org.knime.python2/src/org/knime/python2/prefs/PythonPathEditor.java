/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.python2.prefs;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Activator;

/**
 * Dialog component that allows to select the path to the executable for a specific Python version.
 *
 * @author Clemens von Schwerin, KNIME.com, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class PythonPathEditor extends Composite {

    private final SettingsModelString m_pathConfig;

    private final Label m_header;

    private final Label m_info;

    private final Label m_error;

    /**
     * @param pathConfig The settings model for the path.
     * @param headerLabel The text of the header for the path editor's enclosing group box.
     * @param editorLabel The description text for the path editor.
     * @param parent The parent widget.
     */
    public PythonPathEditor(final SettingsModelString pathConfig, final String headerLabel, final String editorLabel,
        final Composite parent) {
        super(parent, SWT.NONE);
        m_pathConfig = pathConfig;

        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = SWT.FILL;
        setLayoutData(gridData);

        // Header:
        m_header = new Label(this, SWT.NONE);
        FontDescriptor descriptor = FontDescriptor.createFrom(m_header.getFont());
        descriptor = descriptor.setStyle(SWT.BOLD);
        m_header.setFont(descriptor.createFont(m_header.getDisplay()));
        m_header.setText(headerLabel);
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        m_header.setLayoutData(gridData);

        // Path editor:
        final FileFieldEditor pathEditor = new FileFieldEditor(Activator.PLUGIN_ID, editorLabel, this);
        pathEditor.setStringValue(pathConfig.getStringValue());
        pathConfig.addChangeListener(e -> pathEditor.setStringValue(pathConfig.getStringValue()));
        pathEditor.getTextControl(this).addListener(SWT.Traverse, event -> {
            pathConfig.setStringValue(pathEditor.getStringValue());
            if (event.detail == SWT.TRAVERSE_RETURN) {
                event.doit = false;
            }
        });
        pathEditor.setPropertyChangeListener(event -> pathConfig.setStringValue(pathEditor.getStringValue()));

        // Info label:
        m_info = new Label(this, SWT.NONE);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        gridData.verticalIndent = 20;
        m_info.setLayoutData(gridData);

        // Error label:
        m_error = new Label(this, SWT.NONE);
        final Color red = new Color(parent.getDisplay(), 255, 0, 0);
        m_error.setForeground(red);
        m_error.addDisposeListener(e -> red.dispose());
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        m_error.setLayoutData(gridData);
    }

    /**
     * @return The config that holds the path displayed and manipulated by this editor.
     */
    public SettingsModelString getPathConfig() {
        return m_pathConfig;
    }

    public void setDisplayAsDefault(final boolean setAsDefault) {
        final String defaultSuffix = " (Default)";
        final String oldHeaderText = m_header.getText();
        if (setAsDefault) {
            if (!oldHeaderText.endsWith(defaultSuffix)) {
                m_header.setText(oldHeaderText + defaultSuffix);
                layout();
            }
        } else {
            final int suffixStart = oldHeaderText.indexOf(defaultSuffix);
            if (suffixStart != -1) {
                m_header.setText(oldHeaderText.substring(0, suffixStart));
                layout();
            }
        }
    }

    /**
     * Sets the info message of this path editor.
     *
     * @param info The info message.
     */
    public void setInfo(final String info) {
        if (info == null) {
            m_info.setText("");
        } else {
            m_info.setText(info);
        }
        layout();
    }

    /**
     * Sets the error message of this path editor.
     *
     * @param error The error message.
     */
    public void setError(final String error) {
        if (error != null) {
            m_error.setText(error);
        } else {
            m_error.setText("");
        }
        layout();
    }
}
