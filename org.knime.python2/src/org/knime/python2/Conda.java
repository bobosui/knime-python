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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.FutureTask;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.NodeLogger;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class Conda {

    private static final String ROOT_ENVIRONMENT_NAME = "base";

    private static final String DEFAULT_PYTHON2_ENV_PREFIX = "py2_knime";

    private static final String DEFAULT_PYTHON3_ENV_PREFIX = "py3_knime";

    private final String m_pathToExecutable;

    /**
     * Lazily initialized by {@link #getEnvironments()}.
     */
    private String m_rootPrefix = null;

    /**
     * @param pathToExecutable The path to the conda executable.
     * @throws InvalidPathException If the given path is of an invalid format.
     * @throws FileNotFoundException If the given path does not point to an existing file.
     * @throws SecurityException If the conda executable cannot be read or executed by this application.
     * @throws IOException If an I/O error occurs while resolving a symbolic link.
     */
    public Conda(String pathToExecutable) throws FileNotFoundException, IOException {
        final File executableFile;
        // TODO: Not only check file system but also PATH.
        try {
            pathToExecutable = resolveSymbolicLink(pathToExecutable);
            executableFile = new File(pathToExecutable);
            if (!executableFile.exists()) {
                throw new FileNotFoundException("The given path does not point to an existing file.");
            }
        } catch (SecurityException ex) {
            throw new SecurityException("The file at the given path cannot be read. "
                + "Make sure KNIME has the proper access rights for the file.", ex);
        }

        boolean canExecute = true;
        try {
            if (!executableFile.canExecute() && !executableFile.setExecutable(true)) {
                canExecute = false;
            }
        } catch (SecurityException ex) {
            NodeLogger.getLogger(Conda.class).debug(ex, ex);
            canExecute = false;
        }
        if (!canExecute) {
            throw new SecurityException("The file at the given path cannot be executed. "
                + "Make sure to mark it as executable (Mac, Linux) and make sure KNIME has the proper access rights "
                + "for the file.");
        }

        try {
            pathToExecutable = executableFile.getAbsolutePath();
        } catch (SecurityException ex) {
            // Stick with non-absolute path.
        }
        m_pathToExecutable = pathToExecutable;
    }

    private static String resolveSymbolicLink(String pathToExecutable) throws IOException {
        final Path pathObjectToExecutable = Paths.get(pathToExecutable);
        try {
            if (Files.isSymbolicLink(pathObjectToExecutable)) {
                pathToExecutable = Files.readSymbolicLink(pathObjectToExecutable).toString();
            }
        } catch (IOException ex) {
            NodeLogger.getLogger(Conda.class).debug(ex, ex);
            throw new IOException("An error occured while resolving the given symbolic link. "
                + "Please retry using a path that does not point to a symbolic link.", ex);
        }
        return pathToExecutable;
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
     * @return The name of the created conda environment.
     * @throws IOException If an error occurs during execution of the underlying conda commands.
     * @throws UnsupportedOperationException If creating a default environment is not supported for the local operating
     *             system.
     */
    public String createDefaultPython2Environment(final CondaEnvironmentCreationMonitor monitor) throws IOException {
        final String osSubDirectory = getConfigSubDirectoryForOS();
        final String relativePathToDescriptionFile =
            Paths.get("conda-configs", osSubDirectory, "py27_knime.yml").toString();
        String pathToDescriptionFile =
            Activator.getFile(Activator.PLUGIN_ID, relativePathToDescriptionFile).getAbsolutePath();
        return createEnvironmentFromFile(pathToDescriptionFile, PythonVersion.PYTHON2, monitor);
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
            final String osName = SystemUtils.OS_NAME;
            if (osName == null) {
                throw new UnsupportedOperationException(
                    "Could not detect your operating system. This is necessary for environment generation. "
                        + "Please make sure KNIME has the proper access rights to your system.");
            } else {
                throw new UnsupportedOperationException(
                    "Environment generation is only supported for Windows, Mac, and Linux. Your operating system is: "
                        + SystemUtils.OS_NAME);
            }
        }
        return osSubDirectory;
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
     */
    private String createEnvironmentFromFile(final String pathToFile, final PythonVersion pythonVersion,
        final CondaEnvironmentCreationMonitor monitor) throws IOException {
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
        final CondaEnvironmentCreationMonitor monitor) throws IOException {
        final List<String> arguments = new ArrayList<>(6);
        Collections.addAll(arguments, "env", "create", "--file", pathToFile, "--json");
        if (optionalEnvironmentName != null) {
            Collections.addAll(arguments, "--name", optionalEnvironmentName);
        }
        callCondaAndMonitorExecution(monitor, arguments.toArray(new String[0]));
    }

    private void callCondaAndMonitorExecution(final CondaExecutionMonitor monitor, final String... arguments)
        throws IOException {
        final Process conda = startCondaProcess(arguments);

        final FutureTask<Void> outputListener = new FutureTask<>(() -> {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conda.getInputStream()));
            String message;
            try {
                while (!Thread.interrupted() && (message = reader.readLine()) != null
                    && (message = message.trim()) != "") {
                    monitor.handleOutputLine(message);
                }
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return null;
        });

        final FutureTask<Void> errorListener = new FutureTask<>(() -> {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conda.getErrorStream()));
            String message;
            try {
                while (!Thread.interrupted() && (message = reader.readLine()) != null
                    && (message = message.trim()) != "") {
                    monitor.handleErrorLine(message);
                }
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return null;
        });

        outputListener.run();
        errorListener.run();
    }

    private String callCondaAndAwaitTermination(final String... arguments) throws IOException {
        final Process conda = startCondaProcess(arguments);
        // Get regular output.
        final StringWriter outputWriter = new StringWriter();
        IOUtils.copy(conda.getInputStream(), outputWriter, "UTF-8");
        final String testOutput = outputWriter.toString();
        // Get error output.
        final StringWriter errorWriter = new StringWriter();
        IOUtils.copy(conda.getErrorStream(), errorWriter, "UTF-8");
        String errorOutput = errorWriter.toString();
        if (!errorOutput.isEmpty() && !isWarning(errorOutput)) {
            throw new IOException("An error occurred while running conda:\n" + errorOutput);
        }
        return testOutput;
    }

    private Process startCondaProcess(final String... arguments) throws IOException {
        final List<String> argumentList = new ArrayList<>(1 + arguments.length);
        argumentList.add(m_pathToExecutable);
        Collections.addAll(argumentList, arguments);
        final ProcessBuilder pb = new ProcessBuilder(argumentList);
        return pb.start();
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

    abstract static class CondaExecutionMonitor {

        protected abstract void handleOutputLine(String message);

        protected abstract void handleErrorLine(String message);
    }

    public abstract static class CondaEnvironmentCreationMonitor extends CondaExecutionMonitor {

        protected abstract void handleCreationProgress(final String currentPackage, final double progress);

        @Override
        protected final void handleOutputLine(final String message) {
            try (final JsonReader reader = Json.createReader(new StringReader(message))) {
                final JsonObject jsonOutput = reader.readObject();
                final String currentPackage = jsonOutput.getString("fetch");
                final double maxValue = Double.parseDouble(jsonOutput.getString("maxval"));
                final double progress = Double.parseDouble(jsonOutput.getString("progress"));
                handleCreationProgress(currentPackage, progress / maxValue);
            }
        }
    }
}
