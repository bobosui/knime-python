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
import org.knime.python2.Conda.CondaEnvironmentCreationMonitor;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class CondaEnvironmentGenerationDialog extends Dialog {

    public CondaEnvironmentGenerationDialog(final Shell parent) {
        super(parent, SWT.NONE);
    }

    public void open() {
        final Shell parent = getParent();
        final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.SHEET);
        shell.setText("New Conda environment...");
        createContents(shell);
        shell.pack();
        shell.open();
        final Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private static void createContents(final Shell shell) {
        shell.setLayout(new GridLayout(2, false));

        final Label descriptionText = new Label(shell, SWT.WRAP);
        descriptionText.setText("Creating the conda environment may take several minutes"
            + "\nand requires an active internet connection.");
        descriptionText.setData(new GridData());

        final Composite installationMonitorContainer = new Composite(shell, SWT.NONE);
        installationMonitorContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
        installationMonitorContainer.setLayout(new GridLayout());

        // TODO: Python 2 vs Python 3

        final Label statusLabel = new Label(installationMonitorContainer, SWT.NONE);
        statusLabel.setText("Status: "); // TODO: Dummy
        GridData gridData = new GridData();
        gridData.verticalIndent = 10;
        statusLabel.setLayoutData(gridData);

        final Composite progressBarContainer = new Composite(installationMonitorContainer, SWT.NONE);
        progressBarContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
        progressBarContainer.setLayout(new FillLayout());
        final ProgressBar progressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH);
        progressBar.setMaximum(100);
        progressBar.setSelection(50); // TODO: Dummy

        createTextBoxLabel("Conda output log", installationMonitorContainer);
        createTextBox("dummy", false, installationMonitorContainer); // TODO: dummy

        createTextBoxLabel("Conda error log", installationMonitorContainer);
        createTextBox("dummy", true, installationMonitorContainer); // TODO: dummy

        final Button createButton = new Button(shell, SWT.NONE);
        createButton.setText("Create new environment");
        createButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));

        final Button cancelButton = new Button(shell, SWT.NONE);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));

        // Initial state:

        installationMonitorContainer.setEnabled(false);

        // Hooks:

        createButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                createButton.setEnabled(false);
                installationMonitorContainer.setEnabled(true);
                // TODO: Start installation.
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });

        cancelButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                // TODO: Cancel installation if in progress.
                shell.close();
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
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
        final GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.grabExcessHorizontalSpace = true;
        textBox.setLayoutData(gridData);
        return textBox;
    }

    private static final class DialogUpdatingCondaEnvironmentCreationMonitor extends CondaEnvironmentCreationMonitor {

        private final Label m_statusLabel;

        private final ProgressBar m_downloadProgressBar;

        private final Text m_outputTextBox;

        private final Text m_errorTextBox;

        private DialogUpdatingCondaEnvironmentCreationMonitor(final Label statusLabel,
            final ProgressBar downloadProgressBar, final Text outputTextBox, final Text errorTextBox) {
            m_statusLabel = statusLabel;
            m_downloadProgressBar = downloadProgressBar;
            m_outputTextBox = outputTextBox;
            m_errorTextBox = errorTextBox;
        }

        @Override
        protected void handlePackageDownloadProgress(final String currentPackage, final double progress) {
            performActionOnWidgetInUiThread(m_statusLabel, () -> {
                m_statusLabel.setText("Status: Downloading package '" + currentPackage + "'...");
                return null;
            }, true);
            performActionOnWidgetInUiThread(m_downloadProgressBar, () -> {
                m_downloadProgressBar.setSelection((int)(progress * 100));
                return null;
            }, true);
        }

        @Override
        protected void handleNonProgressOutputLine(final String line) {
            performActionOnWidgetInUiThread(m_outputTextBox, () -> {
                m_outputTextBox.setText(m_outputTextBox.getText() + line + "\n");
                return null;
            }, true);
            performActionOnWidgetInUiThread(m_statusLabel, () -> {
                m_statusLabel.setText("Status: Creating Conda environment...");
                return null;
            }, true);
        }

        @Override
        protected void handleErrorLine(final String line) {
            performActionOnWidgetInUiThread(m_errorTextBox, () -> {
                m_errorTextBox.setText(m_errorTextBox.getText() + line + "\n");
                return null;
            }, true);
            performActionOnWidgetInUiThread(m_statusLabel, () -> {
                m_statusLabel.setText("Status: An error occurred. See below.");
                return null;
            }, true);
        }
    }
}
