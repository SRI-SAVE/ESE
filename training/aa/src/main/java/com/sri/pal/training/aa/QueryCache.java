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

// $Id: QueryCache.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.aa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sri.pal.common.SimpleTypeName;

/**
 * Concurrent cache for results of queries sent to an application.
 */
public class QueryCache {
    private final Map<Key, Value> map;

    public QueryCache() {
        map = new HashMap<Key, Value>();
    }

    public Object getOrLock(SimpleTypeName predName,
                            Object[] args)
            throws InterruptedException {
        Key key = new Key(predName, args);
        synchronized (map) {
            if (map.containsKey(key)) {
                while (map.get(key) == null) {
                    map.wait();
                }
                return map.get(key).getResult();
            } else {
                map.put(key, null);
                return null;
            }
        }
    }

    public void add(SimpleTypeName predName,
                    Object[] args,
                    Object result) {
        Key key = new Key(predName, args);
        Value value = new Value(result);
        synchronized (map) {
            map.put(key, value);
            map.notifyAll();
        }
    }

    private static class Key {
        private final SimpleTypeName name;
        private final Object[] args;

        public Key(SimpleTypeName name,
                   Object[] args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(args);
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!Arrays.equals(args, other.args))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    private static class Value {
        private final Object result;

        public Value(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }
}
