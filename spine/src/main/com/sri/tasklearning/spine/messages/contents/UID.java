/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id: UID.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.tasklearning.spine.messages.contents;

import java.io.Serializable;

public class UID
        implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DELIM = ":";

    private final String originator;
    private final int id;

    public UID(String originator,
               int id) {
        this.originator = originator;
        this.id = id;
    }

    public UID(String uidString) {
        int pos = uidString.lastIndexOf(DELIM);
        originator = uidString.substring(0,pos);
        String idStr = uidString.substring(pos + DELIM.length());
        id = Integer.parseInt(idStr);
    }

    public String getOriginator() {
        return originator;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return originator + DELIM + id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((originator == null) ? 0 : originator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UID other = (UID) obj;
        if (id != other.id) {
            return false;
        }
        if (originator == null) {
            if (other.originator != null) {
                return false;
            }
        } else if (!originator.equals(other.originator)) {
            return false;
        }
        return true;
    }
}
