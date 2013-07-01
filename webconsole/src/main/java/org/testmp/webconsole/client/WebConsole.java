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

import org.testmp.webconsole.shared.WebConsoleClientConfig;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Label;
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

        final Label header = new Label();
        header.setIcon("testmp-logo.png");
        header.setIconWidth(265);
        header.setIconHeight(82);
        header.setWidth("95%");
        header.setHeight(72);
        header.setValign(VerticalAlignment.TOP);
        header.setLayoutAlign(Alignment.CENTER);
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

        Tab welcomeTab = new Tab(WebConsoleClientConfig.messages.welcome());
        welcomeTab.setName("welcomeTab");
        welcomeTab.setIcon("welcome.png");
        welcomeTab.setWidth(120);
        VLayout introLayout = new VLayout();
        HTMLPane introView = new HTMLPane();
        introView.setContentsURL(GWT.getModuleBaseURL() + "intro.html");
        introView.setSize("100%", "100%");
        introView.setMargin(10);
        introView.setLayoutAlign(Alignment.CENTER);
        introLayout.addMember(introView);
        welcomeTab.setPane(introLayout);
        appTabSet.addTab(welcomeTab);

        Tab testCaseTab = new Tab(WebConsoleClientConfig.messages.testCase());
        testCaseTab.setName("testCaseTab");
        testCaseTab.setWidth(120);
        testCaseTab.setPane(new TestCaseView());
        appTabSet.addTab(testCaseTab);

        Tab testDataTab = new Tab(WebConsoleClientConfig.messages.testData());
        testDataTab.setName("testDataTab");
        testDataTab.setWidth(120);
        testDataTab.setPane(new TestDataView());
        appTabSet.addTab(testDataTab);

        Tab testEnvTab = new Tab(WebConsoleClientConfig.messages.testEnvironment());
        testEnvTab.setName("testEnvTab");
        testEnvTab.setWidth(120);
        testEnvTab.setPane(new TestEnvView());
        appTabSet.addTab(testEnvTab);

        vLayout.draw();
    }
}
