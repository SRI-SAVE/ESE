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

// $Id: CustomType1.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal;

public class CustomType1 {
    private static int serial = 0;

    private String value;

    public static int getSerial() {
        return serial;
    }

    public CustomType1(String value) {
        this.value = value;
        serial++;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object oth) {
        if (oth instanceof CustomType1 &&
            ((CustomType1)oth).value.equals(value))
            return true;

        return false;
    }

    @Override
    public int hashCode() {
        if (value != null)
            return value.hashCode();

        return -1;
    }
}
