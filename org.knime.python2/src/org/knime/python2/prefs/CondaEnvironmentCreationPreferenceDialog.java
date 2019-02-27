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

import javax.swing.event.ChangeEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.python2.config.CondaEnvironmentCreationDialog;
import org.knime.python2.config.CondaEnvironmentCreationStatus;
import org.knime.python2.config.CondaEnvironmentCreationStatus.CondaEnvironmentCreationStatusListener;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class CondaEnvironmentCreationPreferenceDialog implements CondaEnvironmentCreationDialog {

    private static final String CREATE_BUTTON_TEXT = "Create new environment";

    private final CondaEnvironmentCreationStatus m_status;

    private final Shell m_parent;

    CondaEnvironmentCreationPreferenceDialog(final CondaEnvironmentCreationStatus status, final Shell parent) {
        m_status = status;
        m_parent = parent;
    }

    @Override
    public void open() {
        new CondaEnvironmentCreationPreferenceDialogInstance(m_status, m_parent).open();
    }

    private static class CondaEnvironmentCreationPreferenceDialogInstance extends Dialog {

        private final CondaEnvironmentCreationStatus m_status;

        private final Shell m_shell;

        private Label m_statusLabel;

        private StackLayout m_progressBarStackLayout;

        private ProgressBar m_indeterminateProgressBar;

        private ProgressBar m_determinateProgressBar;

        private Text m_outputTextBox;

        private Text m_errorTextBox;

        private Button m_cancelButton;

        private CondaEnvironmentCreationStatusListener m_environmentCreationListener;

        private CondaEnvironmentCreationPreferenceDialogInstance(final CondaEnvironmentCreationStatus status,
            final Shell parent) {
            super(parent, SWT.NONE);
            m_status = status;
            m_shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.SHEET);
            m_shell.setText("New Conda environment...");
            createContents();
            m_shell.pack();
        }

        private void createContents() {
            m_shell.setLayout(new GridLayout());

            final Label descriptionText = new Label(m_shell, SWT.WRAP);
            descriptionText.setText("Creating the Conda environment may take several minutes"
                + "\nand requires an active internet connection.");

            // Progress monitoring widgets:

            final Composite installationMonitorContainer = new Composite(m_shell, SWT.NONE);
            installationMonitorContainer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
            installationMonitorContainer.setLayout(new GridLayout());

            m_statusLabel = new Label(installationMonitorContainer, SWT.WRAP);
            String statusMessage = m_status.getStatusMessage().getStringValue();
            if (statusMessage == null || statusMessage.isEmpty()) {
                statusMessage = "Please click '" + CREATE_BUTTON_TEXT + "' to start.";
            }
            m_statusLabel.setText(statusMessage);
            GridData gridData = new GridData();
            gridData.verticalIndent = 10;
            m_statusLabel.setLayoutData(gridData);

            final Composite progressBarContainer = new Composite(installationMonitorContainer, SWT.NONE);
            progressBarContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
            m_progressBarStackLayout = new StackLayout();
            progressBarContainer.setLayout(m_progressBarStackLayout);
            m_determinateProgressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH);
            m_determinateProgressBar.setMaximum(100);
            m_determinateProgressBar.setSelection(m_status.getProgress().getIntValue());
            m_indeterminateProgressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH | SWT.INDETERMINATE);
            m_progressBarStackLayout.topControl = m_indeterminateProgressBar;

            createTextBoxLabel("Conda output log", installationMonitorContainer);
            m_outputTextBox =
                createTextBox(m_status.getOutputLog().getStringValue(), false, installationMonitorContainer);

            createTextBoxLabel("Conda error log", installationMonitorContainer);
            m_errorTextBox = createTextBox(m_status.getErrorLog().getStringValue(), true, installationMonitorContainer);

            m_indeterminateProgressBar.setEnabled(false);
            m_outputTextBox.setEnabled(false);
            m_errorTextBox.setEnabled(false);

            // --

            final Composite buttonContainer = new Composite(m_shell, SWT.NONE);
            gridData = new GridData();
            gridData.horizontalAlignment = SWT.RIGHT;
            gridData.verticalIndent = 15;
            buttonContainer.setLayoutData(gridData);
            buttonContainer.setLayout(new RowLayout());
            final Button createButton = createButton(CREATE_BUTTON_TEXT, buttonContainer);
            m_cancelButton = createButton("Cancel", buttonContainer);

            createButton.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    createButton.setEnabled(false);
                    m_indeterminateProgressBar.setEnabled(true);
                    m_outputTextBox.setEnabled(true);
                    m_errorTextBox.setEnabled(true);
                    m_status.startEnvironmentGeneration();
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    widgetSelected(e);
                }
            });

            m_cancelButton.addSelectionListener(new SelectionListener() {

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

        private static Text createTextBox(final String textBoxText, final boolean isErrorTextBox,
            final Composite parent) {
            final Composite textBoxContainer = new Composite(parent, SWT.NONE);
            final GridData gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
            gridData.heightHint = 80;
            textBoxContainer.setLayoutData(gridData);
            textBoxContainer.setLayout(new FillLayout());
            final Text textBox = new Text(textBoxContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
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
            return button;
        }

        private void open() {
            try {
                registerExternalHooks();
                m_shell.open();
                final Display display = getParent().getDisplay();
                while (!m_shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            } finally {
                unregisterExternalHooks();
            }
        }

        private void registerExternalHooks() {
            m_status.getStatusMessage().addChangeListener(this::updateStatusMessage);
            m_status.getProgress().addChangeListener(this::updateProgress);
            m_status.getOutputLog().addChangeListener(this::updateOutputLog);
            m_status.getErrorLog().addChangeListener(this::updateErrorLog);
            m_environmentCreationListener = new CondaEnvironmentCreationStatusListener() {

                @Override
                public void condaEnvironmentCreationStarting() {
                    // no-op
                }

                @Override
                public void condaEnvironmentCreationFinished(final String createdEnvironmentName) {
                    performActionOnWidgetInUiThread(m_shell, () -> {
                        m_shell.close();
                        return null;
                    }, true);
                }

                @Override
                public void condaEnvironmentCreationCanceled() {
                    // no-op
                }

                @Override
                public void condaEnvironmentCreationFailed(final String errorMessage) {
                    performActionOnWidgetInUiThread(m_cancelButton, () -> {
                        m_cancelButton.setText("Close");
                        return null;
                    }, true);
                }
            };
            m_status.addEnvironmentCreationStatusListener(m_environmentCreationListener);
        }

        private void unregisterExternalHooks() {
            m_status.getStatusMessage().removeChangeListener(this::updateStatusMessage);
            m_status.getProgress().removeChangeListener(this::updateProgress);
            m_status.getOutputLog().removeChangeListener(this::updateOutputLog);
            m_status.getErrorLog().removeChangeListener(this::updateErrorLog);
            m_status.removeEnvironmentCreationStatusListener(m_environmentCreationListener);
        }

        private void updateStatusMessage(@SuppressWarnings("unused") final ChangeEvent e) {
            performActionOnWidgetInUiThread(m_statusLabel, () -> {
                m_statusLabel.setText(m_status.getStatusMessage().getStringValue());
                m_statusLabel.requestLayout();
                return null;
            }, true);
        }

        private void updateProgress(@SuppressWarnings("unused") final ChangeEvent e) {
            final int progress = m_status.getProgress().getIntValue();
            Control newVisibleProgressBar;
            if (progress < 100) {
                performActionOnWidgetInUiThread(m_determinateProgressBar, () -> {
                    m_determinateProgressBar.setSelection(progress);
                    m_determinateProgressBar.requestLayout();
                    return null;
                }, true);
                newVisibleProgressBar = m_determinateProgressBar;
            } else {
                newVisibleProgressBar = m_indeterminateProgressBar;
            }
            if (m_progressBarStackLayout.topControl != newVisibleProgressBar) {
                m_progressBarStackLayout.topControl = newVisibleProgressBar;
                performActionOnWidgetInUiThread(m_shell, () -> {
                    m_shell.layout(true, true);
                    return null;
                }, true);
            }
        }

        private void updateOutputLog(@SuppressWarnings("unused") final ChangeEvent e) {
            performActionOnWidgetInUiThread(m_outputTextBox, () -> {
                m_outputTextBox.setText(m_status.getOutputLog().getStringValue());
                m_outputTextBox.requestLayout();
                return null;
            }, true);
        }

        private void updateErrorLog(@SuppressWarnings("unused") final ChangeEvent e) {
            performActionOnWidgetInUiThread(m_errorTextBox, () -> {
                m_errorTextBox.setText(m_status.getErrorLog().getStringValue());
                m_errorTextBox.requestLayout();
                return null;
            }, true);
        }
    }
}