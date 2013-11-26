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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.layout.VLayout;

public class MainPageView extends VLayout {

    @Override
    protected void onInit() {
        super.onInit();
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
        addMember(introView);
    }

}
