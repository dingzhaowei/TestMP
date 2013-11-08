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

import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.DataStoreClient;
import org.testmp.datastore.client.MetaInfo;

public class DataLoader<T> {

    private String url;

    private Class<T> type;

    private DataAssemblyStrategy strategy;

    public DataLoader(String url, Class<T> type, DataAssemblyStrategy strategy) {
        this.url = url;
        this.type = type;
        this.strategy = strategy;
    }

    public List<Map<String, Object>> load(String... tags) {
        try {
            DataStoreClient client = new DataStoreClient(url);
            List<DataInfo<T>> dataInfoList = null;
            if (tags.length == 0) {
                dataInfoList = client.getDataByRange(type, 0, Integer.MAX_VALUE);
            } else {
                dataInfoList = client.getDataByTag(type, tags);
            }
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

            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (DataInfo<T> dataInfo : dataInfoList) {
                MetaInfo metaInfo = metaInfoLookingup.get(dataInfo.getId());
                Map<String, Object> m = strategy.assemble(dataInfo, metaInfo);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load data", e);
        }
    }

}
