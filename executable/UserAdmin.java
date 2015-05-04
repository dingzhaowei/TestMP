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
import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;

public class UserAdmin {

    @SuppressWarnings("unchecked", "rawtypes")
    public static void main(String[] args) throws Exception {
        Logger.getRootLogger().addAppender(new NullAppender());
        String testmpHome = System.getenv("TESTMP_HOME");
        if (testmpHome == null) {
            throw new RuntimeException("TESTMP_HOME is not set");
        }

        Properties props = new Properties();
        String confFile = testmpHome + File.separator + "conf" + File.separator + "testmp.properties";
        props.load(new FileInputStream(confFile));
        String url = props.getProperty("testEnvStoreUrl");
        DataStoreClient client = new DataStoreClient(url);

        String op = args[0];
        String userName = args.length > 1 ? args[1] : null;
        String password = args.length > 2 ? args[2] : null;

        if (op.equals("list")) {
            List<DataInfo<Map>> dataInfoList = client.getDataByTag(Map.class, "User");
            for (DataInfo<Map> dataInfo : dataInfoList) {
                Map user = dataInfo.getData();
                int id = dataInfo.getId();
                String userName = (String) user.get("userName");
                String password = (String) user.get("password");
                System.out.println(String.format("%5d %30s %15s", id, userName, password));
            }
        } else if (op.equals("add")) {
            DataInfo<Map> dataInfo = new DataInfo<Map>();
            Map user = new HashMap();
            user.put("userName", userName);
            user.put("password", password);
            dataInfo.setData(user);
            dataInfo.setTags(Arrays.asList("User"));
            int id = client.addData(dataInfo).get(0);
            System.out.println(id);
        }
    }
}
