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

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.Conda;
import org.knime.python2.Conda.CondaEnvironmentCreationMonitor;
import org.knime.python2.PythonVersion;
import org.knime.python2.kernel.PythonCanceledExecutionException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class CondaEnvironmentCreationStatus {

    private CopyOnWriteArrayList<CondaEnvironmentCreationStatusListener> m_listeners = new CopyOnWriteArrayList<>();

    private static final String IN_PROGRESS_MESSAGE = "Creating Conda environment...";

    private static final String DEFAULT_STRING_VALUE = "";

    private static final int DEFAULT_INT_VALUE = 0;

    private final PythonVersion m_pythonVersion;

    private final SettingsModelString m_condaDirectoryPath;

    // Not meant for saving/loading. We just want observable values here to communicate with the view:

    private static final String DUMMY_CFG_KEY = "dummy";

    private final SettingsModelString m_statusMessage = new SettingsModelString(DUMMY_CFG_KEY, DEFAULT_STRING_VALUE);

    private final SettingsModelInteger m_progress = new SettingsModelInteger(DUMMY_CFG_KEY, DEFAULT_INT_VALUE);

    private final SettingsModelString m_outputLog = new SettingsModelString(DUMMY_CFG_KEY, DEFAULT_STRING_VALUE);

    private final SettingsModelString m_errorLog = new SettingsModelString(DUMMY_CFG_KEY, DEFAULT_STRING_VALUE);

    private CondaEnvironmentCreationMonitor m_monitor;

    public CondaEnvironmentCreationStatus(final PythonVersion environmentPythonVersion,
        final SettingsModelString condaDirectoryPath) {
        m_pythonVersion = environmentPythonVersion;
        m_condaDirectoryPath = condaDirectoryPath;
    }

    /**
     * @return The status message of the current environment creation process.
     */
    public SettingsModelString getStatusMessage() {
        return m_statusMessage;
    }

    /**
     * @return The package download progress of the current environment creation process.
     */
    public SettingsModelInteger getProgress() {
        return m_progress;
    }

    /**
     * @return The output log of th current environment creation process.
     */
    public SettingsModelString getOutputLog() {
        return m_outputLog;
    }

    /**
     * @return The error log of the current environment creation process.
     */
    public SettingsModelString getErrorLog() {
        return m_errorLog;
    }

    public synchronized void startEnvironmentGeneration() {
        if (m_monitor != null) {
            throw new IllegalStateException("Environment generation was tried to be started although one is already in "
                + "progress. This is an implementation error.");
        }
        new Thread(() -> {
            try {
                onEnvironmentCreationStarting();
                final Conda conda = new Conda(m_condaDirectoryPath.getStringValue());
                m_monitor = new StateUpdatingCondaEnvironmentCreationMonitor();
                final String createdEnvironmentName;
                if (m_pythonVersion.equals(PythonVersion.PYTHON2)) {
                    createdEnvironmentName = conda.createDefaultPython2Environment(m_monitor);
                } else if (m_pythonVersion.equals(PythonVersion.PYTHON3)) {
                    createdEnvironmentName = conda.createDefaultPython3Environment(m_monitor);
                } else {
                    throw new IllegalStateException("Python version '" + m_pythonVersion
                        + "' is neither Python 2 nor Python " + "3. This is an implementation error.");
                }
                onEnvironmentCreationFinished(createdEnvironmentName);
            } catch (final PythonCanceledExecutionException ex) {
                onEnvironmentCreationCanceled();
            } catch (final Exception ex) {
                NodeLogger.getLogger(CondaEnvironmentCreationStatus.class).debug(ex, ex);
                onEnvironmentCreationFailed(ex.getMessage());
            } finally {
                resetStatus();
                m_monitor = null;
            }
        }).start();
    }

    public synchronized void cancelEnvironmentGeneration() {
        if (m_monitor != null) {
            m_monitor.cancel();
        }
    }

    /**
     * @param listener A listener which will be notified about changes in the status of the any environment creation
     *            process started by this instance.
     */
    public void addEnvironmentCreationStatusListener(final CondaEnvironmentCreationStatusListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    /**
     * @param listener The listener to remove.
     * @return {@code true} if the listener was present before removal.
     */
    public boolean removeEnvironmentCreationStatusListener(final CondaEnvironmentCreationStatusListener listener) {
        return m_listeners.remove(listener);
    }

    private void resetStatus() {
        m_statusMessage.setStringValue(DEFAULT_STRING_VALUE);
        m_progress.setIntValue(DEFAULT_INT_VALUE);
        m_outputLog.setStringValue(DEFAULT_STRING_VALUE);
        m_errorLog.setStringValue(DEFAULT_STRING_VALUE);
    }

    private void onEnvironmentCreationStarting() {
        m_statusMessage.setStringValue(IN_PROGRESS_MESSAGE);
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationStarting();
        }
    }

    private void onEnvironmentCreationFinished(final String createdEnvironmentName) {
        m_statusMessage.setStringValue(
            "Environment creation finished.\nNew environment's name: '" + createdEnvironmentName + "'.");
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationFinished(createdEnvironmentName);
        }
    }

    private void onEnvironmentCreationCanceled() {
        m_statusMessage.setStringValue("Environment creation was canceled.");
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationCanceled();
        }
    }

    private void onEnvironmentCreationFailed(final String errorMessage) {
        m_statusMessage.setStringValue("Environment creation failed: " + errorMessage);
        for (final CondaEnvironmentCreationStatusListener listener : m_listeners) {
            listener.condaEnvironmentCreationFailed(errorMessage);
        }
    }

    /**
     * Listener which will be notified about changes in the status of environment creation initiated by the enclosing
     * class.
     */
    public interface CondaEnvironmentCreationStatusListener {

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         */
        void condaEnvironmentCreationStarting();

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         *
         * @param createdEnvironmentName The name of the created environment.
         */
        void condaEnvironmentCreationFinished(String createdEnvironmentName);

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         */
        void condaEnvironmentCreationCanceled();

        /**
         * Called asynchronously, that is, possibly not in a UI thread.
         *
         * @param errorMessage The message of the error that made environment creation fail.
         */
        void condaEnvironmentCreationFailed(String errorMessage);
    }

    private final class StateUpdatingCondaEnvironmentCreationMonitor extends CondaEnvironmentCreationMonitor {

        @Override
        protected void handlePackageDownloadProgress(final String currentPackage, final double progress) {
            if (progress < 1.0 - 0.001) {
                m_statusMessage.setStringValue("Downloading package '" + currentPackage + "'...");
            } else {
                m_statusMessage.setStringValue(IN_PROGRESS_MESSAGE);
            }
            m_progress.setIntValue((int)(progress * 100));
        }

        @Override
        protected void handleNonProgressOutputLine(final String line) {
            m_outputLog.setStringValue(m_outputLog.getStringValue() + line + "\n");
            m_statusMessage.setStringValue(IN_PROGRESS_MESSAGE);
        }

        @Override
        protected void handleErrorLine(final String line) {
            m_errorLog.setStringValue(m_errorLog.getStringValue() + line + "\n");
        }
    }
}
