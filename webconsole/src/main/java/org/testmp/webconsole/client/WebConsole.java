/*
 * TestMP (Test Management Platform)
 * Copyright 2013 and beyond, Zhaowei Ding.
 *
 * TestMP is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License (MIT).
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.testmp.webconsole.client;

import java.util.HashMap;
import java.util.Map;

import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.IconButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point for Web Console module.
 */
public class WebConsole implements EntryPoint {

    private Map<String, DataSource> dataSources;

    private IconButton loginBtn;

    private IconButton settingBtn;

    public static DataSource createDataSource(String dataSourceId, String servicePath) {
        DataSource ds = new RestDataSource();
        ds.setID(dataSourceId);
        ds.setDataFormat(DSDataFormat.JSON);

        String baseUrl = GWT.getModuleBaseURL();
        String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
        ds.setDataURL(requestUrl);

        OperationBinding fetch = new OperationBinding();
        fetch.setOperationType(DSOperationType.FETCH);
        fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
        fetch.setDataFormat(DSDataFormat.JSON);

        OperationBinding update = new OperationBinding();
        update.setOperationType(DSOperationType.UPDATE);
        update.setDataProtocol(DSProtocol.POSTMESSAGE);
        update.setDataFormat(DSDataFormat.JSON);

        OperationBinding add = new OperationBinding();
        add.setOperationType(DSOperationType.ADD);
        add.setDataProtocol(DSProtocol.POSTMESSAGE);
        add.setDataFormat(DSDataFormat.JSON);

        OperationBinding remove = new OperationBinding();
        remove.setOperationType(DSOperationType.REMOVE);
        remove.setDataProtocol(DSProtocol.POSTMESSAGE);
        remove.setDataFormat(DSDataFormat.JSON);

        ds.setOperationBindings(fetch, update, add, remove);
        return ds;
    }

    /**
     * This is called when the browser loads Application.html.
     */
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable t) {
                System.err.println("--- UNCAUGHT EXCEPTION ---");
                t.printStackTrace();
            }
        });

        prepareDataSources();

        VLayout vLayout = new VLayout();
        vLayout.setMargin(5);
        vLayout.setLayoutMargin(5);
        vLayout.setAlign(Alignment.CENTER);
        vLayout.setSize("100%", "100%");

        HLayout header = new HLayout();
        header.setWidth("95%");
        header.setHeight(72);
        header.setMembersMargin(5);
        header.setLayoutAlign(Alignment.CENTER);

        Label logo = new Label();
        logo.setIcon("testmp-logo.png");
        logo.setIconWidth(265);
        logo.setIconHeight(82);
        logo.setWidth("95%");
        logo.setValign(VerticalAlignment.TOP);
        logo.setLayoutAlign(Alignment.CENTER);
        header.addMember(logo);

        loginBtn = new IconButton(ClientConfig.messages.login());
        loginBtn.setIcon("person.png");
        loginBtn.setLayoutAlign(VerticalAlignment.TOP);
        loginBtn.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                LoginWindow window = new LoginWindow();
                window.show();
            }

        });
        header.addMember(loginBtn);

        settingBtn = new IconButton(ClientConfig.messages.settings());
        settingBtn.setIcon("setting.png");
        settingBtn.setLayoutAlign(VerticalAlignment.TOP);
        settingBtn.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                if (ClientConfig.currentUser != null) {
                    SettingWindow window = new SettingWindow();
                    window.show();
                } else {
                    SC.say(ClientConfig.messages.nullUser());
                }
            }

        });
        header.addMember(settingBtn);

        vLayout.addMember(header);

        TabSet appTabSet = new TabSet();
        appTabSet.setTabBarPosition(Side.TOP);
        appTabSet.setTabBarAlign(Side.RIGHT);
        appTabSet.setWidth("95%");
        appTabSet.setLayoutAlign(Alignment.CENTER);
        vLayout.addMember(appTabSet);

        String copyright = "<span style=\"font-family:Arial;\">&copy;</span>2013 Zhaowei Ding";
        String licenseLink = "http://opensource.org/licenses/MIT";
        String license = "Licensed under the <a href='" + licenseLink + "'>the MIT License</a>.";

        HTMLFlow footer = new HTMLFlow(copyright + ", " + license);
        footer.setWidth100();
        footer.setHeight(10);
        footer.setStyleName("footing");
        footer.setMargin(5);
        vLayout.addMember(footer);

        Tab welcomeTab = new Tab(ClientConfig.messages.welcome());
        welcomeTab.setName("welcomeTab");
        welcomeTab.setIcon("welcome.png");
        welcomeTab.setWidth(120);
        welcomeTab.setPane(new MainPageView());
        appTabSet.addTab(welcomeTab);

        Tab testCaseTab = new Tab(ClientConfig.messages.testCase());
        testCaseTab.setName("testCaseTab");
        testCaseTab.setWidth(120);
        testCaseTab.setPane(new TestCaseView());
        appTabSet.addTab(testCaseTab);

        Tab testDataTab = new Tab(ClientConfig.messages.testData());
        testDataTab.setName("testDataTab");
        testDataTab.setWidth(120);
        testDataTab.setPane(new TestDataView());
        appTabSet.addTab(testDataTab);

        Tab testEnvTab = new Tab(ClientConfig.messages.testEnvironment());
        testEnvTab.setName("testEnvTab");
        testEnvTab.setWidth(120);
        testEnvTab.setPane(new TestEnvView());
        appTabSet.addTab(testEnvTab);

        vLayout.draw();
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource userNameSource = createDataSource("userNameDS", ClientConfig.constants.userService());
        DataSourceTextField userNameField = new DataSourceTextField("name");
        userNameField.setRequired(true);
        userNameField.setPrimaryKey(true);
        userNameSource.setFields(userNameField);
        dataSources.put("userNameDS", userNameSource);

        DataSource personalSettingsSource = createDataSource("personalSettingsDS", ClientConfig.constants.userService());
        DataSourceTextField fullNameField = new DataSourceTextField("fullName", ClientConfig.messages.fullName());
        DataSourceTextField emailField = new DataSourceTextField("email", ClientConfig.messages.email());
        personalSettingsSource.setFields(fullNameField, emailField);
        dataSources.put("personalSettingsDS", personalSettingsSource);

        DataSource tmrReportSettingsSource = createDataSource("tmrReportSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField tmrRecipientsField = new DataSourceTextField("tmrRecipients",
                ClientConfig.messages.recipients());
        DataSourceTextField tmrSubjectField = new DataSourceTextField("tmrSubject", ClientConfig.messages.subject());
        tmrReportSettingsSource.setFields(tmrRecipientsField, tmrSubjectField);
        dataSources.put("tmrReportSettingsDS", tmrReportSettingsSource);

        DataSource darReportSettingsSource = createDataSource("darReportSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField darRecipientsField = new DataSourceTextField("darRecipients",
                ClientConfig.messages.recipients());
        DataSourceTextField darSubjectField = new DataSourceTextField("darSubjet", ClientConfig.messages.subject());
        darReportSettingsSource.setFields(darRecipientsField, darSubjectField);
        dataSources.put("darReportSettingsDS", darReportSettingsSource);

        DataSource esrReportSettingsSource = createDataSource("esrReportSettingsDS",
                ClientConfig.constants.userService());
        DataSourceTextField esrRecipientsField = new DataSourceTextField("esrRecipients",
                ClientConfig.messages.recipients());
        DataSourceTextField esrSubjectField = new DataSourceTextField("esrSubject", ClientConfig.messages.subject());
        esrReportSettingsSource.setFields(esrRecipientsField, esrSubjectField);
        dataSources.put("esrReportSettingsDS", esrReportSettingsSource);

        DataSource mailSettingsSource = createDataSource("mailSettingsDS", ClientConfig.messages.mailSettings());
        DataSourceTextField smtpSettingUserField = new DataSourceTextField("smtpSettingUser",
                ClientConfig.messages.user());
        DataSourceTextField smtpSettingHostField = new DataSourceTextField("smtpSettingHost",
                ClientConfig.messages.smtpHost());
        DataSourceIntegerField smtpSettingPortField = new DataSourceIntegerField("smtpSettingPort",
                ClientConfig.messages.smtpPort());
        DataSourceBooleanField smtpSettingSTARTTLSField = new DataSourceBooleanField("smtpSettingSTARTTLS",
                ClientConfig.messages.useStarttls());
        mailSettingsSource.setFields(smtpSettingUserField, smtpSettingHostField, smtpSettingPortField,
                smtpSettingSTARTTLSField);
        dataSources.put("mailSettingsDS", mailSettingsSource);
    }

    public class LoginWindow extends Window {

        public LoginWindow() {
            setWidth(300);
            setHeight(100);
            setTitle(ClientConfig.messages.login());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            form.setWidth100();
            ComboBoxItem userNameItem = new ComboBoxItem("name");
            userNameItem.setTitle(ClientConfig.messages.user());
            userNameItem.setOptionDataSource(dataSources.get("userNameDS"));
            userNameItem.setValidators(new CustomValidator() {

                @Override
                protected boolean condition(Object value) {
                    return value != null && value.toString().trim().length() <= 50;
                }

            });
            form.setFields(userNameItem);
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        final String name = form.getValueAsString("name").trim();
                        LoginWindow.this.destroy();
                        Criteria criteria = new Criteria();
                        dataSources.get("userNameDS").fetchData(criteria, new DSCallback() {

                            @Override
                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                Record[] records = response.getData();
                                for (Record record : records) {
                                    if (record.getAttribute("name").equals(name)) {
                                        return;
                                    }
                                }
                                Record record = new Record();
                                record.setAttribute("name", name);
                                dataSources.get("userNameDS").addData(record);
                            }

                        });
                        loginBtn.setTitle(ClientConfig.messages.hi() + ", " + name);
                        ClientConfig.currentUser = name;
                    }
                }

            });
            controls.addMember(okButton);
        }
    }

    public class SettingWindow extends Window {

        public SettingWindow() {
            setWidth(700);
            setHeight(400);
            setTitle(ClientConfig.messages.settings());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            SectionStack sectionStack = new SectionStack();
            sectionStack.setWidth("99%");
            layout.addMember(sectionStack);

            SectionStackSection personalInfoSection = new SectionStackSection();
            personalInfoSection.setTitle(ClientConfig.messages.personalInfo());
            personalInfoSection.setItems(createPersonalSettingsForm());
            sectionStack.addSection(personalInfoSection);

            SectionStackSection reportSettingsSection = new SectionStackSection();
            reportSettingsSection.setTitle(ClientConfig.messages.reportSettings());
            reportSettingsSection.setItems(createReportSettingsForm());
            sectionStack.addSection(reportSettingsSection);

            SectionStackSection mailSettingsSection = new SectionStackSection();
            mailSettingsSection.setTitle(ClientConfig.messages.mailSettings());
            mailSettingsSection.setItems(createMailSettingsForm());
            sectionStack.addSection(mailSettingsSection);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    SettingWindow.this.destroy();
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    SettingWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

        private Canvas createPersonalSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm form = new DynamicForm();
            form.setSize("90%", "33%");
            form.setDataSource(dataSources.get("personalSettingsDS"));
            form.setAutoFetchData(true);
            layout.addMember(form);

            return layout;
        }

        private Canvas createReportSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm tmrForm = new DynamicForm();
            tmrForm.setGroupTitle("Test Metrics");
            tmrForm.setIsGroup(true);
            tmrForm.setSize("90%", "33%");
            tmrForm.setDataSource(dataSources.get("tmrReportSettingsDS"));
            // tmrForm.setAutoFetchData(true);
            layout.addMember(tmrForm);

            DynamicForm darForm = new DynamicForm();
            darForm.setGroupTitle("Data Analytics");
            darForm.setIsGroup(true);
            darForm.setSize("90%", "33%");
            darForm.setDataSource(dataSources.get("darReportSettingsDS"));
            // darForm.setAutoFetchData(true);
            layout.addMember(darForm);

            DynamicForm esrForm = new DynamicForm();
            esrForm.setGroupTitle("Envrionment Status");
            esrForm.setIsGroup(true);
            esrForm.setSize("90%", "33%");
            esrForm.setDataSource(dataSources.get("esrReportSettingsDS"));
            // esrForm.setAutoFetchData(true);
            layout.addMember(esrForm);

            return layout;
        }

        private Canvas createMailSettingsForm() {
            VLayout layout = new VLayout();
            layout.setWidth100();
            layout.setMargin(5);
            layout.setMembersMargin(5);

            DynamicForm form = new DynamicForm();
            form.setMargin(5);
            form.setSize("90%", "33%");
            form.setDataSource(dataSources.get("mailSettingsDS"));
            // form.setAutoFetchData(true);
            layout.addMember(form);

            return layout;
        }
    }
}
