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
        final Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
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
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        shell.setLayout(gridLayout);

        final Label descriptionText = new Label(shell, SWT.WRAP);
        descriptionText.setText("Creating the conda environment may take several minutes"
            + "\nand requires an active internet connection.");
        GridData gridData = new GridData();
        descriptionText.setData(gridData);

        final Label statusLabel = new Label(shell, SWT.NONE);
        statusLabel.setText("Status: ");
        gridData = new GridData();
        gridData.horizontalSpan = 3;
        statusLabel.setLayoutData(gridData);

        final Composite progressBarComposite = new Composite(shell, SWT.NONE);
        progressBarComposite.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, false, false, 3, 1));
        progressBarComposite.setLayout(new FillLayout());
        final ProgressBar progressBar = new ProgressBar(progressBarComposite, SWT.SMOOTH);
        progressBar.setMaximum(100);

        final Label outputTextBoxLabel = new Label(shell, SWT.NONE);
        outputTextBoxLabel.setText("Conda output log");

        final Text outputTextBox = new Text(shell, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        outputTextBox.setText("dummy");
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.horizontalSpan = 3;
        outputTextBox.setLayoutData(gridData);

        final Label errorTextBoxLabel = new Label(shell, SWT.NONE);
        errorTextBoxLabel.setText("Conda error log");

        final Text errorTextBox = new Text(shell, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        final Color red = new Color(shell.getDisplay(), 255, 0, 0);
        errorTextBox.setForeground(red);
        errorTextBox.addDisposeListener(e -> red.dispose());
        errorTextBox.setText("dummy");
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        gridData.horizontalSpan = 3;
        errorTextBox.setLayoutData(gridData);

        final Button startButton = new Button(shell, SWT.NONE);
        startButton.setText("Start");
        startButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 1, 1));

        final Button cancelButton = new Button(shell, SWT.NONE);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false, 1, 1));

        // Hooks:

        startButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
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
}
