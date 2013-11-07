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

import java.util.Map;

import org.testmp.datastore.client.DataInfo;
import org.testmp.datastore.client.MetaInfo;

public interface DataAssemblyStrategy {

    Map<String, Object> assemble(DataInfo<? extends Object> dataInfo, MetaInfo metaInfo);

}
