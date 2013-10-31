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
import org.testmp.webconsole.client.Messages;

import com.google.gwt.core.client.GWT;

public final class ClientConfig {

    public final static Messages messages = GWT.create(Messages.class);

    public final static Constants constants = GWT.create(Constants.class);

    public static String currentUser;

}
