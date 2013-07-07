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

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;

import org.testmp.datastore.client.DataStoreClient;

public class SaveLoad {

    public static void main(String[] args) throws Exception {
        Logger.getRootLogger().addAppender(new NullAppender());
        String testmpHome = System.getenv("TESTMP_HOME");
        if (testmpHome == null) {
            throw new RuntimeException("TESTMP_HOME is not set");
        }

        Properties props = new Properties();
        String confFile = testmpHome + File.separator + "conf" + File.separator + "testmp.properties";
        props.load(new FileInputStream(confFile));

        String op = args[0];
        String type = args.length > 1 ? args[1] : null;
        String file = args.length > 2 ? args[2] : null;

        if (type == null) {
            System.err.println("Need to specify the datastore type.");
            return;
        }

        if (file == null) {
            System.err.println("Need to specify the file path");
            return;
        }

        String url = null;
        if (type.toLowerCase().equals("testcase")) {
            url = props.getProperty("testCaseStoreUrl");
        } else if (type.toLowerCase().equals("testdata")) {
            url = props.getProperty("testDataStoreUrl");
        } else if (type.toLowerCase().equals("testenv")) {
            url = props.getProperty("testEnvStoreUrl");
        } else {
            System.err.println("The datastore type is invalid");
        }

        DataStoreClient client = new DataStoreClient(url);
        if (op.equals("save")) {
            client.saveDataToFile(file);
        } else if (op.equals("load")) {
            client.uploadDataFromFile(file);
        }
    }

}