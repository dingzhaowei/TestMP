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

package org.testmp.datastore.client;

public class DataStoreClientException extends Exception {

    private static final long serialVersionUID = -7698505121430669298L;

    public DataStoreClientException(String msg) {
        super(msg);
    }

    public DataStoreClientException(Throwable cause) {
        super(cause);
    }

    public DataStoreClientException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
