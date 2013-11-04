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

import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.OperationBinding;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RestDataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSOperationType;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HTMLPane;
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
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point for Web Console module.
 */
public class WebConsole implements EntryPoint {

    private DataSource userNameSource;

    private IconButton loginBtn;

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

        userNameSource = new RestDataSource();
        userNameSource.setID("userNameDS");
        userNameSource.setDataFormat(DSDataFormat.JSON);

        String baseUrl = GWT.getModuleBaseURL();
        String servicePath = ClientConfig.constants.userService();
        String requestUrl = baseUrl + servicePath.substring(servicePath.lastIndexOf('/') + 1);
        userNameSource.setDataURL(requestUrl);

        OperationBinding fetch = new OperationBinding();
        fetch.setOperationType(DSOperationType.FETCH);
        fetch.setDataProtocol(DSProtocol.POSTMESSAGE);
        fetch.setDataFormat(DSDataFormat.JSON);
        OperationBinding add = new OperationBinding();
        add.setOperationType(DSOperationType.ADD);
        add.setDataProtocol(DSProtocol.POSTMESSAGE);
        userNameSource.setOperationBindings(fetch, add);

        DataSourceTextField userNameField = new DataSourceTextField("name");
        userNameField.setRequired(true);
        userNameField.setPrimaryKey(true);
        userNameSource.setFields(userNameField);

        VLayout vLayout = new VLayout();
        vLayout.setMargin(5);
        vLayout.setLayoutMargin(5);
        vLayout.setAlign(Alignment.CENTER);
        vLayout.setSize("100%", "100%");

        HLayout header = new HLayout();
        header.setWidth("95%");
        header.setHeight(72);
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
        VLayout introLayout = new VLayout();
        HTMLPane introView = new HTMLPane();
        String localeName = LocaleInfo.getCurrentLocale().getLocaleName();
        if (localeName.startsWith("zh")) {
            introView.setContentsURL(GWT.getModuleBaseURL() + "intro_zh.html");
        } else {
            introView.setContentsURL(GWT.getModuleBaseURL() + "intro.html");
        }
        introView.setContentsType(ContentsType.PAGE);
        introView.setSize("100%", "100%");
        introView.setMargin(5);
        introView.setLayoutAlign(Alignment.CENTER);
        introView.setEvalScriptBlocks(true);
        introLayout.addMember(introView);
        welcomeTab.setPane(introLayout);
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

    public class LoginWindow extends Window {

        public LoginWindow() {
            setWidth(320);
            setHeight(70);
            setTitle(ClientConfig.messages.login());
            ClientUtil.unifySimpleWindowStyle(this);

            HLayout layout = new HLayout();
            ClientUtil.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            ComboBoxItem userNameItem = new ComboBoxItem("name");
            userNameItem.setTitle(ClientConfig.messages.user());
            userNameItem.setOptionDataSource(userNameSource);
            userNameItem.setValidators(new CustomValidator() {

                @Override
                protected boolean condition(Object value) {
                    if (value == null)
                        return false;
                    String v = value.toString().trim();
                    return !v.equalsIgnoreCase("login") && v.length() <= 50;
                }

            });
            form.setFields(userNameItem);
            layout.addMember(form);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        final String name = form.getValueAsString("name").trim();
                        LoginWindow.this.destroy();
                        Criteria criteria = new Criteria();
                        userNameSource.fetchData(criteria, new DSCallback() {

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
                                userNameSource.addData(record);
                            }

                        });
                        loginBtn.setTitle(ClientConfig.messages.hi() + ", " + name);
                        ClientConfig.currentUser = name;
                    }
                }

            });
            layout.addMember(okButton);
        }
    }
}
