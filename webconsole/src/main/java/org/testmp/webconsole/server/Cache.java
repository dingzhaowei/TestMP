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

package org.testmp.webconsole.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.MetaInfo;

public class Cache {

    private static Map<String, Cache> instances = new TreeMap<String, Cache>();

    private Map<Integer, Map<String, Object>> content = new TreeMap<Integer, Map<String, Object>>();

    private String name;

    private Cache(String name) {
        this.name = name;
    }

    public static Cache getInstance(String name) {
        synchronized (Cache.class) {
            if (instances.containsKey(name)) {
                return instances.get(name);
            }
            Cache instance = new Cache(name);
            instances.put(name, instance);
            return instance;
        }
    }

    public String getName() {
        return name;
    }

    public synchronized Map<Integer, Map<String, Object>> getContent() {
        return content;
    }

    public synchronized void updateContent(Integer id, Map<String, Object> newData) {
        if (newData == null) {
            content.remove(id);
        } else {
            content.put(id, newData);
        }
    }

    public synchronized <T> void load(String url, Class<T> type, DataAssemblyStrategy strategy) {
        try {
            DataStoreClient client = new DataStoreClient(url);
            List<DataInfo<T>> dataInfoList = client.getDataByRange(type, 0, Integer.MAX_VALUE);
            Map<Integer, MetaInfo> metaInfoLookingup = new HashMap<Integer, MetaInfo>();

            if (!dataInfoList.isEmpty()) {
                List<Integer> dataIdList = new ArrayList<Integer>();
                for (DataInfo<T> dataInfo : dataInfoList) {
                    dataIdList.add(dataInfo.getId());
                }

                List<MetaInfo> metaInfoList = client.getMetaInfo(dataIdList.toArray(new Integer[0]));
                for (MetaInfo metaInfo : metaInfoList) {
                    metaInfoLookingup.put(metaInfo.getDataId(), metaInfo);
                }
            }

            for (DataInfo<T> dataInfo : dataInfoList) {
                MetaInfo metaInfo = metaInfoLookingup.get(dataInfo.getId());
                Map<String, Object> m = strategy.assemble(dataInfo, metaInfo);
                content.put(dataInfo.getId(), m);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load data into cache: " + name, e);
        }
    }

    public synchronized void clear() {
        content = new TreeMap<Integer, Map<String, Object>>();
    }
}
