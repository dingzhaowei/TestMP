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

package org.testmp.datastore.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testmp.datastore.model.Data;
import org.testmp.datastore.model.DataInfo;
import org.testmp.datastore.model.MetaInfo;
import org.testmp.datastore.model.Property;
import org.testmp.datastore.model.Tag;

import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.collections.TransactionRunner;
import com.sleepycat.collections.TransactionWorker;
import com.sleepycat.je.DatabaseException;

public final class DataStoreManager {

    private static final DataStoreManager manager = new DataStoreManager();

    private static Logger log = Logger.getLogger(DataStoreManager.class);

    private DataStore dataStore;

    private DataStoreView dataStoreView;

    private TransactionRunner runner;

    private boolean openned = false;

    private DataStoreManager() {
        configExit();
    }

    public static DataStoreManager getInstance() {
        return manager;
    }

    /**
     * Open the data store
     * 
     * @param homeDir
     *            the data store home directory
     * @throws DatabaseException
     */
    public void open(String homeDir) throws DatabaseException {
        log.debug("Open data store on " + homeDir);
        if (openned) {
            throw new RuntimeException("The manager has been open on " + dataStore.getEnv().getHome());
        }
        dataStore = new DataStore(homeDir);
        dataStoreView = new DataStoreView(dataStore);
        runner = new TransactionRunner(dataStore.getEnv());
        openned = true;
    }

    /**
     * Close the data store
     * 
     * @throws DatabaseException
     */
    public void close() throws DatabaseException {
        log.debug("Close data store");
        if (openned && dataStore != null) {
            dataStore.close();
        }
        openned = false;
    }

    public boolean isOpenned() {
        return openned;
    }

    public DataStoreView getDataStoreView() {
        return dataStoreView;
    }

    /**
     * Count all the data
     * 
     * @return
     */
    public int countTotalData() {
        try {
            final List<Integer> result = new ArrayList<Integer>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    result.add(dataStoreView.getDataMap().size());
                }

            });
            return result.isEmpty() ? 0 : result.get(0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all the data with specified tags and properties
     * 
     * @param tags
     * @param propertyIds
     * @return
     */
    public List<DataInfo> getData(final List<String> tags, final List<Integer> propertyIds) {
        log.debug("Get data with tags " + tags + " and property IDs " + propertyIds);
        try {
            final List<DataInfo> result = new ArrayList<DataInfo>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap tagMap = dataStoreView.getTagMap();
                    StoredMap dataMap = dataStoreView.getDataMap();
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    HashSet<Integer> retainedIds = new HashSet<Integer>();

                    for (int i = 0; i < tags.size(); i++) {
                        Tag tag = (Tag) tagMap.get(tags.get(i));
                        log.debug("Filter by tag: " + tag);

                        // If any tag is missing, no data will match
                        if (tag == null) {
                            retainedIds.clear();
                            break;
                        }
                        if (i == 0 && retainedIds.isEmpty()) {
                            retainedIds.addAll(tag.getRelatedDataIds());
                        } else {
                            retainedIds.retainAll(tag.getRelatedDataIds());
                        }
                    }

                    if (tags.isEmpty() || !retainedIds.isEmpty()) {
                        for (int i = 0; i < propertyIds.size(); i++) {
                            Property property = (Property) propertyMap.get(propertyIds.get(i));
                            log.debug("Filter by property: " + property);

                            // If any property is missing, no data will match
                            if (property == null) {
                                retainedIds.clear();
                                break;
                            }
                            if (i == 0 && retainedIds.isEmpty()) {
                                retainedIds.addAll(property.getRelatedDataIds());
                            } else {
                                retainedIds.retainAll(property.getRelatedDataIds());
                            }
                        }
                    }

                    log.debug("Retained IDs: " + retainedIds);
                    for (int id : retainedIds) {
                        Data data = (Data) dataMap.get(id);
                        List<String> relatedTags = new ArrayList<String>(data.getRelatedTags());
                        Map<String, Object> relatedProperties = new HashMap<String, Object>();
                        for (int propertyId : data.getRelatedPropertyIds()) {
                            Property property = (Property) propertyMap.get(propertyId);
                            relatedProperties.put(property.getKey(), property.getValue());
                        }
                        result.add(new DataInfo(id, relatedTags, relatedProperties));
                    }
                }

            });
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get data by id
     * 
     * @param id
     * @return
     */
    public List<DataInfo> getDataById(final int id) {
        log.debug("Get data with id " + id);
        try {
            final List<DataInfo> result = new ArrayList<DataInfo>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap dataMap = dataStoreView.getDataMap();
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    if (dataMap.containsKey(id)) {
                        Data data = (Data) dataMap.get(id);
                        List<String> relatedTags = new ArrayList<String>(data.getRelatedTags());
                        Map<String, Object> relatedProperties = new HashMap<String, Object>();
                        for (int propertyId : data.getRelatedPropertyIds()) {
                            Property property = (Property) propertyMap.get(propertyId);
                            relatedProperties.put(property.getKey(), property.getValue());
                        }
                        result.add(new DataInfo(id, relatedTags, relatedProperties));
                    }
                }

            });
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get data of which the id is between a range
     * 
     * @param startId
     * @param endId
     * @return
     */
    public List<DataInfo> getDataByRange(final int startId, final int endId) {
        log.debug("Get data between range [" + startId + ", " + endId + "]");
        try {
            final List<DataInfo> result = new ArrayList<DataInfo>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredSortedMap dataMap = (StoredSortedMap) dataStoreView.getDataMap();
                    StoredMap propertyMap = dataStoreView.getPropertyMap();

                    @SuppressWarnings("unchecked")
                    List<Integer> idList = new ArrayList<Integer>(dataMap.keySet());
                    Collections.sort(idList);

                    for (int id : idList) {
                        if (id >= startId && id <= endId) {
                            Data data = (Data) dataMap.get(id);
                            List<String> relatedTags = new ArrayList<String>(data.getRelatedTags());
                            Map<String, Object> relatedProperties = new HashMap<String, Object>();
                            for (int propertyId : data.getRelatedPropertyIds()) {
                                Property property = (Property) propertyMap.get(propertyId);
                                relatedProperties.put(property.getKey(), property.getValue());
                            }
                            result.add(new DataInfo(id, relatedTags, relatedProperties));
                        } else {
                            break;
                        }
                    }
                }

            });
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public List<DataInfo> getDataByCriteria(Map<String, Object> criteria) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Add a list of tagged data
     * 
     * @param dataInfoList
     * @return
     */
    public List<Integer> addData(final List<DataInfo> dataInfoList) {
        log.debug("Add " + dataInfoList.size() + " data");
        try {
            final HashSet<Integer> result = new HashSet<Integer>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap tagMap = dataStoreView.getTagMap();
                    StoredMap dataMap = dataStoreView.getDataMap();
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    for (DataInfo dataInfo : dataInfoList) {
                        int dataId = Utils.calculateDataId(dataInfo.getData());
                        Data data = dataMap.containsKey(dataId) ? (Data) dataMap.get(dataId) : new Data(dataId);
                        for (Map.Entry<String, Object> e : dataInfo.getData().entrySet()) {
                            String key = e.getKey();
                            Object value = e.getValue();
                            int propertyId = Utils.calculatePropertyId(key, value);
                            Property property = propertyMap.containsKey(propertyId) ? (Property) propertyMap
                                    .get(propertyId) : new Property(propertyId, key, value);
                            property.addRelatedDataId(dataId);
                            propertyMap.put(propertyId, property);
                            log.debug("Updated property: " + property);
                            data.addRelatedPropertyId(propertyId);
                        }
                        for (String t : dataInfo.getTags()) {
                            Tag tag = tagMap.containsKey(t) ? (Tag) tagMap.get(t) : new Tag(t);
                            tag.addRelatedDataId(dataId);
                            tagMap.put(t, tag);
                            log.debug("Updated tag: " + tag);
                            data.addRelatedTag(t);
                        }
                        log.debug("Updated data: " + data);
                        dataMap.put(dataId, data);
                        result.add(dataId);
                        addMetaInfoToData(dataId, "create_time", getCurrentDate());
                    }
                }

            });
            return new ArrayList<Integer>(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete data specified by the id list
     * 
     * @param idList
     */
    public void deleteData(final List<Integer> idList) {
        log.debug("Delete data of id: " + idList);
        try {
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap tagMap = dataStoreView.getTagMap();
                    StoredMap dataMap = dataStoreView.getDataMap();
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    StoredMap metaInfoMap = dataStoreView.getMetaInfoMap();
                    for (int id : idList) {
                        Data data = (Data) dataMap.get(id);
                        if (data == null) {
                            continue;
                        }
                        dataMap.remove(id);
                        for (String t : data.getRelatedTags()) {
                            Tag tag = (Tag) tagMap.get(t);
                            tag.removeRelatedDataId(id);
                            if (tag.getRelatedDataIds().isEmpty()) {
                                tagMap.remove(t);
                            } else {
                                tagMap.put(t, tag);
                            }
                        }
                        for (int p : data.getRelatedPropertyIds()) {
                            Property property = (Property) propertyMap.get(p);
                            property.removeRelatedDataId(id);
                            if (property.getRelatedDataIds().isEmpty()) {
                                propertyMap.remove(p);
                            } else {
                                propertyMap.put(p, property);
                            }
                        }
                        metaInfoMap.remove(id);
                    }
                }

            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all the stored tags
     * 
     * @return
     */
    public List<Tag> getTags() {
        log.debug("Get all tags");
        try {
            final List<Tag> result = new ArrayList<Tag>();
            runner.run(new TransactionWorker() {

                @SuppressWarnings("unchecked")
                public void doWork() throws Exception {
                    result.addAll(dataStoreView.getTagMap().values());
                }

            });
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add tag to data of specified id
     * 
     * @param dataId
     * @param tag
     */
    public void addTagToData(final int dataId, final String tag) {
        log.debug("Add tag " + tag + " to data of id " + dataId);
        try {
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap dataMap = dataStoreView.getDataMap();
                    if (!dataMap.containsKey(dataId)) {
                        throw new IllegalArgumentException("non-existent data id");
                    }
                    Data data = (Data) dataMap.get(dataId);
                    StoredMap tagMap = dataStoreView.getTagMap();
                    Tag t = tagMap.containsKey(tag) ? (Tag) tagMap.get(tag) : new Tag(tag);
                    t.addRelatedDataId(dataId);
                    tagMap.put(tag, t);
                    data.addRelatedTag(tag);
                    dataMap.put(dataId, data);
                    addMetaInfoToData(dataId, "last_modify_time", getCurrentDate());
                }

            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete tag from data of specified id
     * 
     * @param dataId
     * @param tag
     */
    public void deleteTagFromData(final int dataId, final String tag) {
        log.debug("Delete tag " + tag + " from data of id " + dataId);
        try {
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap dataMap = dataStoreView.getDataMap();
                    if (!dataMap.containsKey(dataId)) {
                        throw new IllegalArgumentException("non-existent data id");
                    }
                    Data data = (Data) dataMap.get(dataId);
                    if (!data.getRelatedTags().contains(tag)) {
                        throw new IllegalArgumentException("tag doesn't exist in data");
                    }
                    StoredMap tagMap = dataStoreView.getTagMap();
                    Tag t = (Tag) tagMap.get(tag);
                    t.removeRelatedDataId(dataId);
                    if (t.getRelatedDataIds().isEmpty()) {
                        tagMap.remove(tag);
                    } else {
                        tagMap.put(tag, t);
                    }
                    data.removeRelatedTag(tag);
                    dataMap.put(dataId, data);
                    addMetaInfoToData(dataId, "last_modify_time", getCurrentDate());
                }

            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all the stored property values mapping to the specified key
     * 
     * @param key
     * @param tags
     * @return
     */
    List<Object> getPropertyValues(final String key, final List<String> tags) {
        log.debug("Get property values mapping to key (" + key + ") filtered by tags " + tags);
        try {
            final HashSet<Object> result = new HashSet<Object>();
            runner.run(new TransactionWorker() {

                @SuppressWarnings("rawtypes")
                public void doWork() throws Exception {
                    StoredMap propertyMapIndexedByKey = dataStoreView.getPropertyMapIndexedByKey();
                    StoredMap dataMap = dataStoreView.getDataMap();
                    Collection c = propertyMapIndexedByKey.duplicates(key);
                    Iterator iter = c.iterator();
                    while (iter.hasNext()) {
                        Property property = (Property) iter.next();
                        if (tags != null && !tags.isEmpty()) {
                            for (int dataId : property.getRelatedDataIds()) {
                                Data relatedData = (Data) dataMap.get(dataId);
                                if (relatedData.getRelatedTags().containsAll(tags)) {
                                    result.add(property.getValue());
                                    break;
                                }
                            }
                        } else {
                            result.add(property.getValue());
                        }
                    }
                }

            });
            return new ArrayList<Object>(result);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add property to data of specified id
     * 
     * @param dataId
     * @param key
     * @param value
     */
    public void addPropertyToData(final int dataId, final String key, final Object value) {
        log.debug("Add property (" + key + ", " + value + ") to data of id " + dataId);
        try {
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap dataMap = dataStoreView.getDataMap();
                    if (!dataMap.containsKey(dataId)) {
                        throw new IllegalArgumentException("non-existent data id");
                    }
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    Data data = (Data) dataMap.get(dataId);

                    for (int propertyId : data.getRelatedPropertyIds()) {
                        Property property = (Property) propertyMap.get(propertyId);
                        if (property.getKey().equals(key)) {
                            data.removeRelatedPropertyId(propertyId);
                            property.removeRelatedDataId(dataId);
                            if (property.getRelatedDataIds().isEmpty()) {
                                propertyMap.remove(propertyId);
                            } else {
                                propertyMap.put(propertyId, property);
                            }
                            break;
                        }
                    }

                    int propertyId = Utils.calculatePropertyId(key, value);
                    Property property = propertyMap.containsKey(propertyId) ? (Property) propertyMap.get(propertyId)
                            : new Property(propertyId, key, value);
                    property.addRelatedDataId(dataId);
                    propertyMap.put(propertyId, property);
                    data.addRelatedPropertyId(propertyId);

                    dataMap.put(dataId, data);
                    addMetaInfoToData(dataId, "last_modify_time", getCurrentDate());
                }

            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete property from data of specified id
     * 
     * @param dataId
     * @param key
     */
    public void deletePropertyFromData(final int dataId, final String key) {
        log.debug("Delete property " + key + " from data of id " + dataId);
        try {
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap dataMap = dataStoreView.getDataMap();
                    if (!dataMap.containsKey(dataId)) {
                        return;
                    }
                    Data data = (Data) dataMap.get(dataId);
                    StoredMap propertyMap = dataStoreView.getPropertyMap();
                    for (int propertyId : data.getRelatedPropertyIds()) {
                        Property property = (Property) propertyMap.get(propertyId);
                        if (property.getKey().equals(key)) {
                            property.removeRelatedDataId(dataId);
                            if (property.getRelatedDataIds().isEmpty()) {
                                propertyMap.remove(propertyId);
                            } else {
                                propertyMap.put(propertyId, property);
                            }
                            data.removeRelatedPropertyId(propertyId);
                            break;
                        }
                    }
                    dataMap.put(dataId, data);
                    addMetaInfoToData(dataId, "last_modify_time", getCurrentDate());
                }

            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the a list of meta info of data
     * 
     * @param dataIdList
     * @return
     */
    public List<MetaInfo> getMetaInfo(final List<Integer> dataIdList) {
        log.debug("Get the meta info of data: " + dataIdList);
        try {
            final List<MetaInfo> result = new ArrayList<MetaInfo>();
            runner.run(new TransactionWorker() {

                public void doWork() throws Exception {
                    StoredMap metaInfoMap = dataStoreView.getMetaInfoMap();
                    for (Integer dataId : dataIdList) {
                        MetaInfo metaInfo = (MetaInfo) metaInfoMap.get(dataId);
                        if (metaInfo != null) {
                            result.add(metaInfo);
                        }
                    }
                }

            });
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add meta info to a list of data of specified id
     * 
     * @param dataId
     * @param key
     * @param value
     */
    private void addMetaInfoToData(final int dataId, final String key, final Object value) {
        log.debug("Add meta info to data of specified id");
        StoredMap metaInfoMap = dataStoreView.getMetaInfoMap();
        if (metaInfoMap.containsKey(dataId)) {
            MetaInfo metaInfo = (MetaInfo) metaInfoMap.get(dataId);
            metaInfo.addMetaInfo(key, value);
            metaInfoMap.put(dataId, metaInfo);
        } else {
            StoredMap dataMap = dataStoreView.getDataMap();
            if (dataMap.containsKey(dataId)) {
                MetaInfo metaInfo = new MetaInfo();
                metaInfo.setDataId(dataId);
                metaInfo.addMetaInfo(key, value);
                metaInfoMap.put(dataId, metaInfo);
            }
        }
    }

    /**
     * Delete meta info from a list of data of specified id
     * 
     * @param dataId
     * @param key
     */
    @SuppressWarnings("unused")
    private void deleteMetaInfoFromData(final int dataId, final String key) {
        log.debug("Delete meta info from data of specified id");
        StoredMap metaInfoMap = dataStoreView.getMetaInfoMap();
        if (metaInfoMap.containsKey(dataId)) {
            MetaInfo metaInfo = (MetaInfo) metaInfoMap.get(dataId);
            metaInfo.removeMetaInfo(key);
            if (metaInfo.getMetaInfo().isEmpty()) {
                metaInfoMap.remove(dataId);
            } else {
                metaInfoMap.put(dataId, metaInfo);
            }
        }
    }

    private void configExit() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    close();
                } catch (Exception e) {
                    // ignore it
                }
            }
        });
    }

    private String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(currentDate);
    }

}
