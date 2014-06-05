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

package org.testmp.webconsole.model;

import java.util.Set;

public class Team extends User {

    private Set<String> members;

    private Set<String> administers;

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public Set<String> getAdministers() {
        return administers;
    }

    public void setAdministers(Set<String> administers) {
        this.administers = administers;
    }

    public boolean isMember(String name) {
        return members.contains(name);
    }

    public boolean isAdminister(String name) {
        return administers.contains(name);
    }

}
