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

import org.testmp.datastore.model.Data;
import org.testmp.datastore.model.MetaInfo;
import org.testmp.datastore.model.Property;
import org.testmp.datastore.model.Tag;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredSortedMap;

public class DataStoreView {

    private StoredMap dataMap;

    private StoredMap tagMap;

    private StoredMap propertyMap;

    private StoredMap metaInfoMap;

    private StoredMap dataMapIndexedByProps;

    private StoredMap propMapIndexedByKey;

    private StoredMap propMapIndexedByKeyValue;

    public DataStoreView(DataStore db) {
        ClassCatalog catalog = db.getClassCatalog();
        EntryBinding intBinding = new SerialBinding(catalog, Integer.class);
        EntryBinding strBinding = new SerialBinding(catalog, String.class);
        EntryBinding dataBinding = new SerialBinding(catalog, Data.class);
        EntryBinding tagBinding = new SerialBinding(catalog, Tag.class);
        EntryBinding propertyBinding = new SerialBinding(catalog, Property.class);
        EntryBinding metaInfoBinding = new SerialBinding(catalog, MetaInfo.class);
        dataMap = new StoredSortedMap(db.getDataStore(), intBinding, dataBinding, true);
        tagMap = new StoredMap(db.getTagStore(), strBinding, tagBinding, true);
        propertyMap = new StoredSortedMap(db.getPropertyStore(), intBinding, propertyBinding, true);
        metaInfoMap = new StoredMap(db.getMetaInfoStore(), intBinding, metaInfoBinding, true);
        dataMapIndexedByProps = new StoredMap(db.getDataStoreByProperties(), strBinding, dataBinding, true);
        propMapIndexedByKey = new StoredMap(db.getPropertyStoreByKey(), strBinding, propertyBinding, true);
        propMapIndexedByKeyValue = new StoredMap(db.getPropertyStoreByKeyValue(), strBinding, propertyBinding, true);
    }

    public StoredMap getDataMap() {
        return dataMap;
    }

    public StoredMap getTagMap() {
        return tagMap;
    }

    public StoredMap getPropertyMap() {
        return propertyMap;
    }

    public StoredMap getMetaInfoMap() {
        return metaInfoMap;
    }

    public StoredMap getDataMapIndexedByProps() {
        return dataMapIndexedByProps;
    }

    public StoredMap getPropertyMapIndexedByKey() {
        return propMapIndexedByKey;
    }

    public StoredMap getPropMapIndexedByKeyValue() {
        return propMapIndexedByKeyValue;
    }
}
