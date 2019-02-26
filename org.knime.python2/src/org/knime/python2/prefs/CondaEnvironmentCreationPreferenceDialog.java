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
 *   Feb 26, 2019 (marcel): created
 */
package org.knime.python2.prefs;

import static org.knime.python2.prefs.PythonPreferenceUtils.performActionOnWidgetInUiThread;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.python2.config.CondaEnvironmentCreationDialog;
import org.knime.python2.config.CondaEnvironmentCreationStatus;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class CondaEnvironmentCreationPreferenceDialog extends Dialog implements CondaEnvironmentCreationDialog {

    private static final String STATUS_LABEL_PREFIX = "Status: ";

    private final CondaEnvironmentCreationStatus m_status;

    private final Shell m_shell;

    private Label m_statusLabel;

    private ProgressBar m_downloadProgressBar;

    private Text m_outputTextBox;

    private Text m_errorTextBox;

    public CondaEnvironmentCreationPreferenceDialog(final CondaEnvironmentCreationStatus status, final Shell parent) {
        super(parent, SWT.NONE);
        m_status = status;
        m_shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.SHEET);
        m_shell.setText("New Conda environment...");
        createContents();
        m_shell.pack();
    }

    private void createContents() {
        m_shell.setLayout(new GridLayout(2, false));

        final Label descriptionText = new Label(m_shell, SWT.WRAP);
        descriptionText.setText("Creating the conda environment may take several minutes"
            + "\nand requires an active internet connection.");
        GridData gridData = new GridData();
        gridData.horizontalSpan = 2;
        descriptionText.setLayoutData(gridData);

        // Progress monitoring widgets:

        final Composite installationMonitorContainer = new Composite(m_shell, SWT.NONE);
        installationMonitorContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
        installationMonitorContainer.setLayout(new GridLayout());

        m_statusLabel = new Label(installationMonitorContainer, SWT.NONE);
        String statusMessage = m_status.getStatusMessage().getStringValue();
        if (statusMessage == null || statusMessage.isEmpty()) {
            statusMessage = "Not yet started. Click 'Create new environment'.";
        }
        m_statusLabel.setText(STATUS_LABEL_PREFIX + statusMessage);
        gridData = new GridData();
        gridData.verticalIndent = 10;
        m_statusLabel.setLayoutData(gridData);

        final Composite progressBarContainer = new Composite(installationMonitorContainer, SWT.NONE);
        progressBarContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
        progressBarContainer.setLayout(new FillLayout());
        m_downloadProgressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH);
        m_downloadProgressBar.setMaximum(100);
        m_downloadProgressBar.setSelection(m_status.getProgress().getIntValue());

        createTextBoxLabel("Conda output log", installationMonitorContainer);
        m_outputTextBox = createTextBox(m_status.getOutputLog().getStringValue(), false, installationMonitorContainer);

        createTextBoxLabel("Conda error log", installationMonitorContainer);
        m_errorTextBox = createTextBox(m_status.getErrorLog().getStringValue(), true, installationMonitorContainer);

        installationMonitorContainer.setEnabled(false);

        // --

        final Button createButton = createButton("Create new environment", m_shell);
        final Button cancelButton = createButton("Cancel", m_shell);

        // Hooks:

        m_status.getStatusMessage().addChangeListener(e -> updateStatusMessage());
        m_status.getProgress().addChangeListener(e -> updateProgress());
        m_status.getOutputLog().addChangeListener(e -> updateOutputLog());
        m_status.getErrorLog().addChangeListener(e -> updateErrorLog());

        createButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                createButton.setEnabled(false);
                installationMonitorContainer.setEnabled(true);
                m_status.startEnvironmentGeneration();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });

        cancelButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_status.cancelEnvironmentGeneration();
                m_shell.close();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });

        m_shell.addShellListener(new ShellListener() {

            @Override
            public void shellIconified(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellDeiconified(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellDeactivated(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellClosed(final ShellEvent e) {
                m_status.cancelEnvironmentGeneration();
            }

            @Override
            public void shellActivated(final ShellEvent e) {
                // no-op
            }
        });
    }

    private static Label createTextBoxLabel(final String labelText, final Composite parent) {
        final Label textBoxLabel = new Label(parent, SWT.NONE);
        textBoxLabel.setText(labelText);
        final GridData gridData = new GridData();
        gridData.verticalIndent = 10;
        textBoxLabel.setLayoutData(gridData);
        return textBoxLabel;
    }

    private static Text createTextBox(final String textBoxText, final boolean isErrorTextBox, final Composite parent) {
        final Composite textBoxContainer = new Composite(parent, SWT.NONE);
        textBoxContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
        textBoxContainer.setLayout(new FillLayout());
        final Text textBox = new Text(textBoxContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        if (isErrorTextBox) {
            final Color red = new Color(textBoxContainer.getDisplay(), 255, 0, 0);
            textBox.setForeground(red);
            textBox.addDisposeListener(e -> red.dispose());
        }
        textBox.setText(textBoxText);
        return textBox;
    }

    private static Button createButton(final String buttonText, final Composite parent) {
        final Button button = new Button(parent, SWT.NONE);
        button.setText(buttonText);
        final GridData gridData = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
        gridData.verticalIndent = 20;
        button.setLayoutData(gridData);
        return button;
    }

    private void updateStatusMessage() {
        performActionOnWidgetInUiThread(m_statusLabel, () -> {
            m_statusLabel.setText(STATUS_LABEL_PREFIX + m_status.getStatusMessage().getStringValue());
            return null;
        }, true);
    }

    private void updateProgress() {
        performActionOnWidgetInUiThread(m_downloadProgressBar, () -> {
            m_downloadProgressBar.setSelection(m_status.getProgress().getIntValue());
            return null;
        }, true);
    }

    private void updateOutputLog() {
        performActionOnWidgetInUiThread(m_outputTextBox, () -> {
            m_outputTextBox.setText(m_status.getOutputLog().getStringValue());
            return null;
        }, true);
    }

    private void updateErrorLog() {
        performActionOnWidgetInUiThread(m_errorTextBox, () -> {
            m_errorTextBox.setText(m_status.getErrorLog().getStringValue());
            return null;
        }, true);
    }

    @Override
    public void open() {
        m_shell.open();
        final Display display = getParent().getDisplay();
        while (!m_shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
}
