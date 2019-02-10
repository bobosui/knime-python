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
 *   Feb 10, 2019 (marcel): created
 */
package org.knime.python2.prefs;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Conda;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class CondaEnvironmentSelectionBox extends Composite {

    private static final String NO_ENVIRONMENT_PLACEHOLDER = "<no environment>";

    private final SettingsModelString m_environmentConfig;

    private final Label m_header;

    private Combo m_environmentSelection;

    private final Label m_info;

    private final Label m_error;

    /**
     * @param environmentConfig The settings model for the conda environment name.
     * @param pathToCondaExecutable The settings model that contains the path to the conda executable.
     * @param selectionBoxLabel The description text for the environment selection box.
     * @param headerLabel The text of the header for the path editor's enclosing group box.
     * @param parent The parent widget.
     */
    public CondaEnvironmentSelectionBox(final SettingsModelString environmentConfig,
        final SettingsModelString pathToCondaExecutable, final String headerLabel, final String selectionBoxLabel,
        final Composite parent) {
        super(parent, SWT.NONE);
        m_environmentConfig = environmentConfig;

        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        setLayout(gridLayout);

        // Header:
        m_header = new Label(this, SWT.NONE);
        FontDescriptor descriptor = FontDescriptor.createFrom(m_header.getFont());
        descriptor = descriptor.setStyle(SWT.BOLD);
        m_header.setFont(descriptor.createFont(m_header.getDisplay()));
        m_header.setText(headerLabel);
        GridData gridData = new GridData();
        gridData.horizontalSpan = 3;
        m_header.setLayoutData(gridData);

        // Environment selection:
        final Label environmentSelectionLabel = new Label(this, SWT.NONE);
        environmentSelectionLabel.setText(selectionBoxLabel);
        m_environmentSelection = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        clearSelectionToPlaceholder();

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

        // Populate and hook environment selection:
        refreshAvailableEnvironments(pathToCondaExecutable.getStringValue());

        pathToCondaExecutable
            .addChangeListener(e -> refreshAvailableEnvironments(pathToCondaExecutable.getStringValue()));

        environmentConfig.addChangeListener(e -> setSelectedEnvironment(environmentConfig.getStringValue()));
        m_environmentSelection.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                environmentConfig.setStringValue(getSelectedEnvironment());
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });
    }

    /**
     * @return The config that holds the environment name displayed and manipulated by this editor.
     */
    public SettingsModelString getEnvironmentConfig() {
        return m_environmentConfig;
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
     * Sets the info message of this environment selection.
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
     * Sets the error message of this environment selection.
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

    private void refreshAvailableEnvironments(final String pathToCondaExecutable) {
        try {
            final String previousSelection = getSelectedEnvironment();
            final Conda conda = new Conda(pathToCondaExecutable);
            List<String> environments = conda.getEnvironments();
            if (environments.isEmpty()) {
                environments = Arrays.asList(NO_ENVIRONMENT_PLACEHOLDER);
            }
            m_environmentSelection.setItems(environments.toArray(new String[0]));
            setSelectedEnvironment(previousSelection);
        } catch (Exception ex) {
            clearSelectionToPlaceholder();
            setInfo(null);
            final String errorMessage;
            if (ex instanceof FileNotFoundException) {
                // Non-existent executable path will be reported elsewhere, so we stay silent here.
                errorMessage = null;
            } else {
                errorMessage = "Failed to list available conda environments. See log for details.";
                NodeLogger.getLogger(CondaEnvironmentSelectionBox.class).error(ex);
            }
            setError(errorMessage);
        }
    }

    private String getSelectedEnvironment() {
        return m_environmentSelection.getItem(m_environmentSelection.getSelectionIndex());
    }

    private void setSelectedEnvironment(final String environmentName) {
        final int numEnvironments = m_environmentSelection.getItemCount();
        int indexToSelect = -1;
        for (int i = 0; i < numEnvironments; i++) {
            if (m_environmentSelection.getItem(i).equals(environmentName)) {
                indexToSelect = i;
                break;
            }
        }
        if (indexToSelect == -1) {
            if (numEnvironments == 0) {
                m_environmentSelection.setItems(NO_ENVIRONMENT_PLACEHOLDER);
            }
            indexToSelect = 0;
        }
        m_environmentSelection.select(indexToSelect);
    }

    private void clearSelectionToPlaceholder() {
        m_environmentSelection.setItems(NO_ENVIRONMENT_PLACEHOLDER);
        setSelectedEnvironment(NO_ENVIRONMENT_PLACEHOLDER);
    }
}
