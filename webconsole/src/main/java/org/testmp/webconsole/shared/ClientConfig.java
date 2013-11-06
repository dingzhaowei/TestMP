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

package org.testmp.webconsole.shared;

import org.testmp.webconsole.client.Constants;
import org.testmp.webconsole.client.FilterWindow.FilterType;
import org.testmp.webconsole.client.Messages;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.AdvancedCriteria;

public final class ClientConfig {

    public final static Messages messages = GWT.create(Messages.class);

    public final static Constants constants = GWT.create(Constants.class);

    public static String currentUser;

    public static AdvancedCriteria getCurrentFilterCriteria(FilterType filterType) {
        switch (filterType) {
        case TEST_CASE:
            return currentTestCaseCriteria;
        case TEST_DATA:
            return currentTestDataCriteria;
        default:
            return null;
        }
    }

    public static void setCurrentFilterCriteria(AdvancedCriteria criteria, FilterType filterType) {
        switch (filterType) {
        case TEST_CASE:
            currentTestCaseCriteria = criteria;
            break;
        case TEST_DATA:
            currentTestDataCriteria = criteria;
            break;
        default:
            return;
        }
    }

    private static AdvancedCriteria currentTestCaseCriteria = new AdvancedCriteria();

    private static AdvancedCriteria currentTestDataCriteria = new AdvancedCriteria();
}
