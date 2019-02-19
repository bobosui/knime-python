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
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.python2.prefs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.CondaEnvironmentConfig;
import org.knime.python2.config.EnvironmentTypeConfig;
import org.knime.python2.config.ManualEnvironmentConfig;
import org.knime.python2.config.PythonEnvironmentConfigObserver;
import org.knime.python2.config.PythonEnvironmentConfigObserver.PythonEnvironmentConfigTestStatusListener;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonVersionConfig;
import org.knime.python2.config.SerializerConfig;

/**
 * Preference page for configurations related to the org.knime.python2 plug-in.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
public final class PythonPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private Display m_parentDisplay;

    private ScrolledComposite m_containerScrolledView;

    private Composite m_container;

    private List<PreferencePersistor> m_preferencePersistors;

    private Composite m_environmentConfigurationPanel;

    private StackLayout m_environmentConfigurationLayout;

    private CondaEnvironmentPreferencePanel m_condaEnvironmentPanel;

    private ManualEnvironmentPreferencePanel m_manualEnvironmentPanel;

    private PythonEnvironmentConfigObserver m_configObserver;

    @Override
    public void init(final IWorkbench workbench) {
        // no op
    }

    @Override
    protected Control createContents(final Composite parent) {
        createPageBody(parent);
        createInfoHeader(parent);

        m_preferencePersistors = new ArrayList<>(4);

        // Python version selection:

        final PythonVersionConfig pythonVersionConfig = new PythonVersionConfig();
        // Reference to object is not needed here; everything is handled in its constructor.
        @SuppressWarnings("unused")
        final Object unused0 = new PythonVersionPreferencePanel(pythonVersionConfig, m_container);
        m_preferencePersistors.add(new PythonVersionPreferencePersistor(pythonVersionConfig));

        // Environment configuration:

        final Group environmentConfigurationGroup = new Group(m_container, SWT.NONE);
        environmentConfigurationGroup.setText("Python environment configuration");
        environmentConfigurationGroup.setLayout(new GridLayout());
        environmentConfigurationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Environment type selection:

        final EnvironmentTypeConfig environmentTypeConfig = new EnvironmentTypeConfig();
        // Reference to object is not needed here; everything is handled in its constructor.
        @SuppressWarnings("unused")
        final Object unused1 = new EnvironmentTypePreferencePanel(environmentTypeConfig, environmentConfigurationGroup);
        final Label separator = new Label(environmentConfigurationGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        m_environmentConfigurationPanel = new Composite(environmentConfigurationGroup, SWT.NONE);
        m_environmentConfigurationLayout = new StackLayout();
        m_environmentConfigurationPanel.setLayout(m_environmentConfigurationLayout);
        m_environmentConfigurationPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Conda environment configuration:

        final CondaEnvironmentConfig condaEnvironmentConfig = new CondaEnvironmentConfig();
        m_condaEnvironmentPanel =
            new CondaEnvironmentPreferencePanel(condaEnvironmentConfig, m_environmentConfigurationPanel);
        m_preferencePersistors.add(new CondaEnvironmentPreferencePersistor(condaEnvironmentConfig));

        // Manual environment configuration:

        final ManualEnvironmentConfig manualEnvironmentConfig = new ManualEnvironmentConfig();
        m_manualEnvironmentPanel =
            new ManualEnvironmentPreferencePanel(manualEnvironmentConfig, m_environmentConfigurationPanel);
        m_preferencePersistors.add(new ManualEnvironmentPreferencePersistor(manualEnvironmentConfig));

        displayPanelForEnvironmentType(PythonEnvironmentType.CONDA.getId());

        // Serializer selection:

        final SerializerConfig serializerConfig = new SerializerConfig();
        // Reference to object is not needed here; everything is handled in its constructor.
        @SuppressWarnings("unused")
        Object unused2 = new SerializerPreferencePanel(serializerConfig, m_container);
        m_preferencePersistors.add(new SerializerPreferencePersistor(serializerConfig));

        // Load saved configs from preferences:

        loadConfigurations();

        displayDefaultPythonEnvironment(m_manualEnvironmentPanel,
            pythonVersionConfig.getPythonVersion().getStringValue());

        // Hooks:

        pythonVersionConfig.getPythonVersion()
            .addChangeListener(e -> displayDefaultPythonEnvironment(m_manualEnvironmentPanel,
                pythonVersionConfig.getPythonVersion().getStringValue()));

        environmentTypeConfig.getEnvironmentType().addChangeListener(
            e -> displayPanelForEnvironmentType(environmentTypeConfig.getEnvironmentType().getStringValue()));

        m_configObserver = new PythonEnvironmentConfigObserver(environmentTypeConfig, condaEnvironmentConfig,
            manualEnvironmentConfig, serializerConfig);

        // Displaying installation test results may require resizing the scroll view.
        m_configObserver.addConfigTestStatusListener(new PythonEnvironmentConfigTestStatusListener() {

            @Override
            public void condaInstallationTestStarting() {
                updateDisplayMinSize();
            }

            @Override
            public void condaInstallationTestFinished(final String errorMessage) {
                updateDisplayMinSize();
            }

            @Override
            public void environmentInstallationTestStarting(final PythonEnvironmentType environmentType,
                final PythonVersion pythonVersion) {
                updateDisplayMinSize();
            }

            @Override
            public void environmentInstallationTestFinished(final PythonEnvironmentType environmentType,
                final PythonVersion pythonVersion, final PythonKernelTestResult testResult) {
                updateDisplayMinSize();
            }
        });

        // Initial installation test:

        m_configObserver.testSelectedPythonEnvironmentType();

        return m_containerScrolledView;
    }

    private void createPageBody(final Composite parent) {
        m_parentDisplay = parent.getDisplay();
        m_containerScrolledView = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        m_container = new Composite(m_containerScrolledView, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        m_container.setLayout(gridLayout);

        m_containerScrolledView.setContent(m_container);
        m_containerScrolledView.setExpandHorizontal(true);
        m_containerScrolledView.setExpandVertical(true);
    }

    private void createInfoHeader(final Composite parent) {
        final Link startScriptInfo = new Link(m_container, SWT.NONE);
        startScriptInfo.setLayoutData(new GridData());
        final String message = "See <a href=\"https://docs.knime.com/latest/python_installation_guide/index.html\">"
            + "this guide</a> for details on how to install Python for use with KNIME.";
        startScriptInfo.setText(message);
        final Color gray = new Color(parent.getDisplay(), 100, 100, 100);
        startScriptInfo.setForeground(gray);
        startScriptInfo.addDisposeListener(e -> gray.dispose());
        startScriptInfo.setFont(JFaceResources.getFontRegistry().getItalic(""));
        startScriptInfo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                } catch (PartInitException | MalformedURLException ex) {
                    NodeLogger.getLogger(PythonPreferencePage.class).error(ex);
                }
            }
        });
    }

    private static void displayDefaultPythonEnvironment(final ManualEnvironmentPreferencePanel environmentPanel,
        final String pythonVersion) {
        final PythonPathEditor pythonPathEditorToSetDefault;
        final PythonPathEditor pythonPathEditorToUnsetDefault;
        if (PythonVersion.PYTHON2.getId().equals(pythonVersion)) {
            pythonPathEditorToSetDefault = environmentPanel.getPython2PathEditor();
            pythonPathEditorToUnsetDefault = environmentPanel.getPython3PathEditor();
        } else if (PythonVersion.PYTHON3.getId().equals(pythonVersion)) {
            pythonPathEditorToSetDefault = environmentPanel.getPython3PathEditor();
            pythonPathEditorToUnsetDefault = environmentPanel.getPython2PathEditor();
        } else {
            throw new IllegalStateException("Selected default Python version is neither Python 2 nor Python3. "
                + "This is an implementation error.");
        }
        pythonPathEditorToSetDefault.setDisplayAsDefault(true);
        pythonPathEditorToUnsetDefault.setDisplayAsDefault(false);
    }

    private void displayPanelForEnvironmentType(final String environmentTypeId) {
        final PythonEnvironmentType environmentType = PythonEnvironmentType.fromId(environmentTypeId);
        if (PythonEnvironmentType.CONDA.equals(environmentType)) {
            m_environmentConfigurationLayout.topControl = m_condaEnvironmentPanel.getPanel();
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            m_environmentConfigurationLayout.topControl = m_manualEnvironmentPanel.getPanel();
        } else {
            throw new IllegalStateException(
                "Selected Python environment type is neither Conda nor manual. This is an implementation error.");
        }
        m_environmentConfigurationPanel.layout();
    }

    private void updateDisplayMinSize() {
        try {
            m_parentDisplay.syncExec(() -> {
                if (!getControl().isDisposed()) {
                    m_container.layout();
                    m_containerScrolledView.setMinSize(m_container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                }
            });
        } catch (final SWTException ex) {
            // Display or control have been disposed - ignore.
        }
    }

    @Override
    public boolean performOk() {
        saveConfigurations();
        return true;
    }

    @Override
    protected void performApply() {
        saveConfigurations();
        m_configObserver.testSelectedPythonEnvironmentType();
    }

    @Override
    protected void performDefaults() {
        final PreferenceStorage defaultPreferences = PythonPreferences.DEFAULT;
        for (final PreferencePersistor persistor : m_preferencePersistors) {
            persistor.loadSettingsFrom(defaultPreferences);
        }
    }

    /**
     * Saves the preference page's configurations to the preferences.
     */
    private void saveConfigurations() {
        final PreferenceStorage currentPreferences = PythonPreferences.CURRENT;
        for (final PreferencePersistor persistor : m_preferencePersistors) {
            persistor.saveSettingsTo(currentPreferences);
        }
    }

    /**
     * Loads the preference page's configuration from the stored preferences.
     */
    private void loadConfigurations() {
        final PreferenceStorage currentPreferences = PythonPreferences.CURRENT;
        for (final PreferencePersistor persistor : m_preferencePersistors) {
            persistor.loadSettingsFrom(currentPreferences);
        }
    }
}
