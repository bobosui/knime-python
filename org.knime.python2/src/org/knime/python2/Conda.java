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
 *   Feb 2, 2019 (marcel): created
 */
package org.knime.python2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Version;
import org.knime.python2.kernel.PythonCanceledExecutionException;

/**
 * Interface to an external Conda installation.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class Conda {

    private static final Version CONDA_MINIMUM_VERSION = new Version(4, 4, 0);

    private static final String ROOT_ENVIRONMENT_NAME = "base";

    private static final String DEFAULT_PYTHON2_ENV_PREFIX = "py2_knime";

    private static final String DEFAULT_PYTHON3_ENV_PREFIX = "py3_knime";

    private static final String CONDA_CONFIGS_DIRECTORY = "conda-configs";

    private static final String PYTHON2_DESCRIPTION_FILE = "py27_knime.yml";

    private static final String PYTHON3_DESCRIPTION_FILE = "py35_knime.yml";

    /**
     * Creates and returns a {@link PythonCommand} that describes a Python process that is run in the Conda environment
     * identified by the given Conda installation directory and the given Conda environment name.<br>
     * The validity of the given arguments is not tested.
     *
     * @param condaInstallationDirectoryPath The path to the directory of the Conda installation.
     * @param environmentName The name of the Conda environment.
     * @return A command to start a Python process in the given environment using the given Conda installation.
     */
    public static PythonCommand createPythonCommand(final String condaInstallationDirectoryPath,
        final String environmentName) {
        final String osSubDirectory = getConfigSubDirectoryForOS();
        final String osStartScriptFilExtension = getStartScriptFileExtensionForOS();
        final String relativePathToStartScript =
            Paths.get(CONDA_CONFIGS_DIRECTORY, osSubDirectory, "start_py" + "." + osStartScriptFilExtension).toString();
        final String pathToStartScript =
            Activator.getFile(Activator.PLUGIN_ID, relativePathToStartScript).getAbsolutePath();
        return new DefaultPythonCommand(pathToStartScript, condaInstallationDirectoryPath, environmentName);
    }

    private final String m_command;

    /**
     * Lazily initialized by {@link #getEnvironments()}.
     */
    private String m_rootPrefix = null;

    /**
     * Creates an interface to the given Conda installation. Tests the validity of the installation and throws an
     * {@link IOException} if it is invalid.
     *
     * @param condaInstallationDirectoryPath The path to the directory of the Conda installation.
     *
     * @throws IOException If the given directory does not point to a valid Conda installation. This includes cases
     *             where the given directory or any relevant files within that directory cannot be read (and/or possibly
     *             executed) by this application.
     */
    public Conda(String condaInstallationDirectoryPath) throws IOException {
        final File directoryFile = resolvePathToFile(condaInstallationDirectoryPath);
        try {
            condaInstallationDirectoryPath = directoryFile.getAbsolutePath();
        } catch (SecurityException ex) {
            // Stick with the non-absolute path.
            condaInstallationDirectoryPath = directoryFile.getPath();
        }
        m_command = getCommandFromInstallationDirectoryForOS(condaInstallationDirectoryPath);

        testInstallation();
    }

    private static File resolvePathToFile(final String installationDirectoryPath) throws IOException {
        final File installationDirectory = new File(installationDirectoryPath);
        try {
            if (!installationDirectory.exists()) {
                throw new IOException("The directory at the given path does not exist.\nPlease specify the path to "
                    + "the directory of your local Conda installation.");
            }
            if (!installationDirectory.isDirectory()) {
                throw new IOException("The given path does not point to a directory.\nPlease point to the base "
                    + "directory of your local Conda installation.");
            }
        } catch (final SecurityException ex) {
            final String errorMessage = "The directory at the given path cannot be read. Please make sure KNIME has "
                + "the proper access rights for the directory and retry.";
            NodeLogger.getLogger(Conda.class).debug(errorMessage, ex);
            throw new IOException(errorMessage, ex);
        }
        return installationDirectory;
    }

    private static String getCommandFromInstallationDirectoryForOS(final String installationDirectoryPath)
        throws IOException {
        final String[] relativePathToExecutableSegments;
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            relativePathToExecutableSegments = new String[]{"bin", "conda"};
        } else if (SystemUtils.IS_OS_WINDOWS) {
            relativePathToExecutableSegments = new String[]{"Scripts", "conda.exe"};
        } else {
            throw createUnknownOSException();
        }
        try {
            return Paths.get(installationDirectoryPath, relativePathToExecutableSegments).toString();
        } catch (final InvalidPathException ex) {
            final String errorMessage = ex.getMessage() + "\nThis is an implementation error.";
            throw new IOException(errorMessage, ex);
        }
    }

    /**
     * Test conda installation by trying to get its version. Method throws an exception if conda could not be called
     * properly. We also check the version bound since we currently require conda {@link #CONDA_MINIMUM_VERSION} or
     * later.
     *
     * @throws IOException If the installation test failed.
     */
    private void testInstallation() throws IOException {
        String versionString = getVersionString();
        final Version version;
        try {
            // We expect a return value of the form "conda <major>.<minor>.<micro>".
            versionString = versionString.split(" ")[1];
            version = new Version(versionString);
        } catch (Exception ex) {
            // Skip test if we can't identify version.
            NodeLogger.getLogger(Conda.class).warn("Could not detect installed Conda version. Please note that a "
                + "minimum version of " + CONDA_MINIMUM_VERSION + " is required.");
            return;
        }
        if (version.compareTo(CONDA_MINIMUM_VERSION) < 0) {
            throw new IOException("Conda version is " + version.toString() + ". Required minimum version is "
                + CONDA_MINIMUM_VERSION + ". Please update Conda and retry.");
        }
    }

    /**
     * {@code conda --version}
     *
     * @return The raw output of the corresponding conda command.
     * @throws IOException If an error occurs during execution of the underlying command.
     */
    public String getVersionString() throws IOException {
        return callCondaAndAwaitTermination("--version");
    }

    /**
     * {@code conda env list}
     *
     * @return The names of the existing conda environments.
     * @throws IOException If an error occurs during execution of the underlying command.
     */
    public List<String> getEnvironments() throws IOException {
        if (m_rootPrefix == null) {
            m_rootPrefix = getRootPrefix();
        }
        final String jsonOutput = callCondaAndAwaitTermination("env", "list", "--json");
        try (final JsonReader reader = Json.createReader(new StringReader(jsonOutput))) {
            final JsonArray environmentsJson = reader.readObject().getJsonArray("envs");
            final List<String> environments = new ArrayList<>(environmentsJson.size());
            for (int i = 0; i < environmentsJson.size(); i++) {
                final String environmentPath = environmentsJson.getString(i);
                final String environmentName;
                if (environmentPath.equals(m_rootPrefix)) {
                    environmentName = ROOT_ENVIRONMENT_NAME;
                } else {
                    environmentName = new File(environmentPath).getName();
                }
                environments.add(environmentName);
            }
            return environments;
        }
    }

    private String getRootPrefix() throws IOException {
        final String jsonOutput = callCondaAndAwaitTermination("info", "--json");
        try (final JsonReader reader = Json.createReader(new StringReader(jsonOutput))) {
            return reader.readObject().getString("root_prefix");
        }
    }

    /**
     * Creates a new Python 2 conda environment of a unique name that contains all packages required by the KNIME Python
     * integration.
     *
     * @param monitor Receives progress of the creation process. Allows to cancel environment creation.
     * @return The name of the created conda environment.
     * @throws IOException If an error occurs during execution of the underlying conda commands.
     * @throws PythonCanceledExecutionException If environment creation was canceled via the given monitor.
     * @throws UnsupportedOperationException If creating a default environment is not supported for the local operating
     *             system.
     */
    public String createDefaultPython2Environment(final CondaEnvironmentCreationMonitor monitor)
        throws IOException, PythonCanceledExecutionException {
        return createDefaultPythonEnvironment(PYTHON2_DESCRIPTION_FILE, PythonVersion.PYTHON2, monitor);
    }

    /**
     * Creates a new Python 3 conda environment of a unique name that contains all packages required by the KNIME Python
     * integration.
     *
     * @param monitor Receives progress of the creation process. Allows to cancel environment creation.
     * @return The name of the created conda environment.
     * @throws IOException If an error occurs during execution of the underlying conda commands.
     * @throws PythonCanceledExecutionException If environment creation was canceled via the given monitor.
     * @throws UnsupportedOperationException If creating a default environment is not supported for the local operating
     *             system.
     */
    public String createDefaultPython3Environment(final CondaEnvironmentCreationMonitor monitor)
        throws IOException, PythonCanceledExecutionException {
        return createDefaultPythonEnvironment(PYTHON3_DESCRIPTION_FILE, PythonVersion.PYTHON3, monitor);
    }

    private String createDefaultPythonEnvironment(final String descriptionFileName, final PythonVersion pythonVersion,
        final CondaEnvironmentCreationMonitor monitor) throws IOException, PythonCanceledExecutionException {
        final String osSubDirectory = getConfigSubDirectoryForOS();
        final String relativePathToDescriptionFile =
            Paths.get(CONDA_CONFIGS_DIRECTORY, osSubDirectory, descriptionFileName).toString();
        final String pathToDescriptionFile =
            Activator.getFile(Activator.PLUGIN_ID, relativePathToDescriptionFile).getAbsolutePath();
        return createEnvironmentFromFile(pathToDescriptionFile, pythonVersion, monitor);
    }

    /**
     * {@code conda env create --file <file>}.<br>
     * The environment name specified in the file is ignored and replaced by a unique name that considers the already
     * existing environments of this conda installation. The generated name is based on the given Python version.
     *
     * @param pathToFile The path to the environment description file.
     * @param pythonVersion The major version of the Python environment to create. Determines the generated name of the
     *            environment.
     * @return The name of the created environment.
     * @throws IOException If an error occurs during execution of the underlying command.
     * @throws PythonCanceledExecutionException If environment creation was canceled via the given monitor.
     */
    private String createEnvironmentFromFile(final String pathToFile, final PythonVersion pythonVersion,
        final CondaEnvironmentCreationMonitor monitor) throws IOException, PythonCanceledExecutionException {
        final String environmentPrefix = pythonVersion.equals(PythonVersion.PYTHON2) //
            ? DEFAULT_PYTHON2_ENV_PREFIX //
            : DEFAULT_PYTHON3_ENV_PREFIX;
        String environmentName = environmentPrefix;
        long possibleEnvironmentSuffix = 1;
        final List<String> environments = getEnvironments();
        while (environments.contains(environmentName)) {
            environmentName = environmentPrefix + "_" + possibleEnvironmentSuffix;
            possibleEnvironmentSuffix++;
        }
        IOException failure = null;
        try {
            createEnvironmentFromFile(pathToFile, environmentName, monitor);
        } catch (IOException ex) {
            failure = ex;
        }
        // Check if environment creation was successful. Fail if not.
        if (!getEnvironments().contains(environmentName)) {
            if (failure == null) {
                failure = new IOException("Failed to create conda environment.");
            }
            throw failure;
        }
        return environmentName;
    }

    /**
     * {@code conda env create --file <file> [-n <name>]}
     */
    private void createEnvironmentFromFile(final String pathToFile, final String optionalEnvironmentName,
        final CondaEnvironmentCreationMonitor monitor) throws IOException, PythonCanceledExecutionException {
        final List<String> arguments = new ArrayList<>(6);
        Collections.addAll(arguments, "env", "create", "--file", pathToFile, "--json");
        if (optionalEnvironmentName != null) {
            Collections.addAll(arguments, "--name", optionalEnvironmentName);
        }
        callCondaAndMonitorExecution(monitor, arguments.toArray(new String[0]));
    }

    private void callCondaAndMonitorExecution(final CondaExecutionMonitor monitor, final String... arguments)
        throws IOException, PythonCanceledExecutionException {
        final Process conda = startCondaProcess(arguments);
        final Thread outputListener =
            new Thread(createCondaStreamReaderRunnable(conda.getInputStream(), monitor, monitor::handleOutputLine));
        final Thread errorListener =
            new Thread(createCondaStreamReaderRunnable(conda.getErrorStream(), monitor, line -> {
                NodeLogger.getLogger(Conda.class).debug(line);
                monitor.handleErrorLine(line);
            }));
        outputListener.start();
        errorListener.start();

        // TODO: Make cancelable via monitor.
        final int condaExitCode = awaitTermination(conda);
        // Should not be necessary, but let's play safe here.
        outputListener.interrupt();
        errorListener.interrupt();
        if (condaExitCode != 0) {
            throw new IOException("Conda process terminated with error code " + condaExitCode + ".");
        }
    }

    private static Runnable createCondaStreamReaderRunnable(final InputStream stream,
        final CondaExecutionMonitor monitor, final Consumer<String> lineProcessor) {
        return () -> {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            try {
                while (!monitor.isCanceled() && !Thread.interrupted() && (line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line != "") {
                        lineProcessor.accept(line);
                    }
                }
            } catch (final IOException ex) {
                NodeLogger.getLogger(Conda.class).debug(ex.getMessage(), ex);
                throw new UncheckedIOException(ex);
            }
        };
    }

    private String callCondaAndAwaitTermination(final String... arguments) throws IOException {
        final Process conda = startCondaProcess(arguments);
        try {
            // Get regular output.
            final StringWriter outputWriter = new StringWriter();
            IOUtils.copy(conda.getInputStream(), outputWriter, "UTF-8");
            final String testOutput = outputWriter.toString();
            // Get error output.
            final StringWriter errorWriter = new StringWriter();
            IOUtils.copy(conda.getErrorStream(), errorWriter, "UTF-8");
            String errorOutput = errorWriter.toString();

            final int condaExitCode = awaitTermination(conda);
            if (condaExitCode != 0) {
                String errorMessage;
                if (!errorOutput.isEmpty() && !isWarning(errorOutput)) {
                    errorMessage = "Failed to execute conda:\n" + errorOutput;
                } else {
                    errorMessage = "Conda process terminated with error code " + condaExitCode + ".";
                    if (!errorOutput.isEmpty()) {
                        errorMessage += "\nFurther output: " + errorMessage;
                    }
                }
                throw new IOException(errorMessage);
            }
            return testOutput;
        } catch (IOException ex) {
            NodeLogger.getLogger(Conda.class).debug(ex.getMessage(), ex);
            throw ex;
        }
    }

    private Process startCondaProcess(final String... arguments) throws IOException {
        final List<String> argumentList = new ArrayList<>(1 + arguments.length);
        argumentList.add(m_command);
        Collections.addAll(argumentList, arguments);
        final ProcessBuilder pb = new ProcessBuilder(argumentList);
        try {
            return pb.start();
        } catch (IOException ex) {
            NodeLogger.getLogger(Conda.class).debug(ex.getMessage(), ex);
            throw ex;
        }
    }

    private static int awaitTermination(final Process conda) throws IOException {
        try {
            return conda.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("An interrupt occurred while waiting for the conda process to terminate.");
        }
    }

    private static boolean isWarning(String errorMessage) {
        errorMessage = errorMessage.trim();
        if (errorMessage.startsWith("==> WARNING: A newer version of conda exists. <==")) {
            final String[] lines = errorMessage.split("\n");
            final String lastLine = lines[lines.length - 1];
            if (lastLine.trim().startsWith("$ conda update -n base")) {
                return true;
            }
        }
        return false;
    }

    private static String getConfigSubDirectoryForOS() {
        final String osSubDirectory;
        if (SystemUtils.IS_OS_LINUX) {
            osSubDirectory = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            osSubDirectory = "macos";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            osSubDirectory = "windows";
        } else {
            throw createUnknownOSException();
        }
        return osSubDirectory;
    }

    private static String getStartScriptFileExtensionForOS() {
        final String osStartScriptFileExtension;
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) {
            osStartScriptFileExtension = "sh";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            osStartScriptFileExtension = "bat";
        } else {
            throw createUnknownOSException();
        }
        return osStartScriptFileExtension;
    }

    private static UnsupportedOperationException createUnknownOSException() {
        final String osName = SystemUtils.OS_NAME;
        if (osName == null) {
            throw new UnsupportedOperationException(
                "Could not detect your operating system. This is necessary for Conda environment generation and use. "
                    + "Please make sure KNIME has the proper access rights to your system.");
        } else {
            throw new UnsupportedOperationException(
                "Conda environment generation and use is only supported on Windows, Mac, and Linux. Your operating "
                    + "system is: " + SystemUtils.OS_NAME);
        }
    }

    /**
     * Allows to monitor the progress of a conda environment creation command. Conda only reports progress for package
     * downloads.
     */
    public abstract static class CondaEnvironmentCreationMonitor extends CondaExecutionMonitor {

        /**
         * Asynchronous callback that allows to process progress in the download of a Python package.<br>
         * Exceptions thrown by this callback are discarded.
         *
         * @param currentPackage The package for which progress is reported.
         * @param progress The progress as a fraction in [0, 1].
         */
        protected abstract void handlePackageDownloadProgress(final String currentPackage, final double progress);

        /**
         * Asynchronous callback that allows to process a non-error output line at a time that is not a download
         * progress report of the monitored conda command.<br>
         * Exceptions thrown by this callback are discarded.
         *
         * @param line The output message line, neither {@code null} nor empty.
         */
        protected abstract void handleNonProgressOutputLine(String line);

        @Override
        protected final void handleOutputLine(final String line) {
            try (final JsonReader reader = Json.createReader(new StringReader(line))) {
                final JsonObject jsonOutput = reader.readObject();
                final String currentPackage = jsonOutput.getString("fetch");
                final double maxValue = Double.parseDouble(jsonOutput.getString("maxval"));
                final double progress = Double.parseDouble(jsonOutput.getString("progress"));
                handlePackageDownloadProgress(currentPackage, progress / maxValue);
            } catch (final Exception ex) {
                handleNonProgressOutputLine(line);
            }
        }
    }

    abstract static class CondaExecutionMonitor {

        private boolean m_isCanceled;

        /**
         * Asynchronous callback that allows to process a non-error output line at a time of the monitored conda
         * command.<br>
         * Exceptions thrown by this callback are discarded.
         *
         * @param line The output message line, neither {@code null} nor empty.
         */
        protected abstract void handleOutputLine(String line);

        /**
         * Asynchronous callback that allows to process an error output line at a time of the monitored conda
         * command.<br>
         * Exceptions thrown by this callback are discarded.
         *
         * @param line The error message line, neither {@code null} nor empty.
         */
        protected abstract void handleErrorLine(String line);

        /**
         * Cancels the execution of the monitored conda command.
         */
        public synchronized void cancel() {
            m_isCanceled = true;
        }

        /**
         * @return True if the command shall be canceled, false otherwise. Clears the "canceled" flag.
         */
        private synchronized boolean isCanceled() {
            if (m_isCanceled) {
                m_isCanceled = false;
                return true;
            } else {
                return false;
            }
        }
    }
}
