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

package org.testmp.sync;

import java.io.IOException;
import java.util.Properties;

public class TestConfig {

    private static Properties props;

    static {
        ClassLoader cl = TestConfig.class.getClassLoader();
        props = new Properties();
        try {
            props.load(cl.getResourceAsStream("testsync.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = props.getProperty(key);
        }
        return value;
    }

}
