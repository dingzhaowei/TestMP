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
import org.testmp.webconsole.shared.ClientUtil;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.LocaleInfo;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
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
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point for Web Console module.
 */
public class WebConsole implements EntryPoint {

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

        IconButton loginBtn = new IconButton(ClientConfig.messages.login());
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
            setWidth(250);
            setHeight(150);
            setTitle(ClientConfig.messages.login());
            ClientUtil.unifySimpleWindowStyle(this);

            final VLayout layout = new VLayout();
            ClientUtil.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();
            TextItem userItem = new TextItem("username");
            userItem.setTitle(ClientConfig.messages.user());
            userItem.setRequired(true);
            PasswordItem passwordItem = new PasswordItem("password");
            passwordItem.setTitle(ClientConfig.messages.password());
            form.setFields(userItem, passwordItem);
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtil.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        LoginWindow.this.removeItem(layout);
                        final Label loading = ClientUtil.createLoadingLabel();
                        LoginWindow.this.addItem(loading);

                        String username = form.getValueAsString("username");
                        String password = form.getValueAsString("password");
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("username", username.trim());
                        params.put("password", password == null ? "" : password.trim());
                        StringBuilder dataBuilder = new StringBuilder();
                        for (Map.Entry<String, String> param : params.entrySet()) {
                            String key = param.getKey();
                            String value = param.getValue();
                            dataBuilder.append('&').append(key).append('=').append(URL.encode(value));
                        }
                        String data = dataBuilder.toString();
                        String servicePath = ClientConfig.constants.loginService();
                        ClientUtil.sendDataFromWindow(layout, LoginWindow.this, loading, data, servicePath);
                    }
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    LoginWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

    }
}
