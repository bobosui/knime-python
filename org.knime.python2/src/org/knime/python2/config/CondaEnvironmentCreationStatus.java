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
package org.knime.python2.config;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Conda.CondaEnvironmentCreationMonitor;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class CondaEnvironmentCreationStatus {

    private CopyOnWriteArrayList<CondaEnvironmentCreationStatusListener> m_listeners = new CopyOnWriteArrayList<>();

    // Not meant for saving/loading. We just want observable values here to communicate with the view:

    private static final String DUMMY_CFG_KEY = "dummy";

    private final SettingsModelString m_statusMessage = new SettingsModelString(DUMMY_CFG_KEY, "");

    private final SettingsModelInteger m_progress = new SettingsModelInteger(DUMMY_CFG_KEY, 0);

    private final SettingsModelString m_outputLog = new SettingsModelString(DUMMY_CFG_KEY, "");

    private final SettingsModelString m_errorLog = new SettingsModelString(DUMMY_CFG_KEY, "");

    public SettingsModelString getStatusMessage() {
        return m_statusMessage;
    }

    public SettingsModelInteger getProgress() {
        return m_progress;
    }

    public SettingsModelString getOutputLog() {
        return m_outputLog;
    }

    public SettingsModelString getErrorLog() {
        return m_errorLog;
    }

    public void addEnvironmentCreationStatusListener(final CondaEnvironmentCreationStatusListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    public boolean removeEnvironmentCreationStatusListener(final CondaEnvironmentCreationStatusListener listener) {
        return m_listeners.remove(listener);
    }

    public void startEnvironmentGeneration() {
        onEnvironmentCreationStarted();
    }

    public void cancelEnvironmentGeneration() {
        onEnvironmentCreationCanceled();
    }

    private synchronized void onEnvironmentCreationStarted() {
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationStarted();
        }
    }

    private synchronized void onEnvironmentCreationCanceled() {
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationCanceled();
        }
    }

    public interface CondaEnvironmentCreationStatusListener {

        void condaEnvironmentCreationStarted();

        void condaEnvironmentCreationCanceled();
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
                m_statusLabel.setText(STATUS_LABEL_PREFIX + "Downloading package '" + currentPackage + "'...");
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
                m_statusLabel.setText(STATUS_LABEL_PREFIX + "Creating Conda environment...");
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
                m_statusLabel.setText(STATUS_LABEL_PREFIX + "An error occurred. See below.");
                return null;
            }, true);
        }
    }
}
