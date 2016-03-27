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

// $Id: InvalidNullValueException.java,v  $

package com.sri.pal.common;

/**
 * Thrown when a null value of a non-nullable type is encountered;
 * may be thrown by any task learning component.
 */
public class InvalidNullValueException extends Exception {

  static final long serialVersionUID = 1;

  public InvalidNullValueException() {
    super("InvalidNullValueException");
  }

  public InvalidNullValueException(String message) {
    super("InvalidNullValue: " + message);
  }

  public InvalidNullValueException(String message, Throwable cause) {
    super("InvalidNullValue: " + message, cause);
  }

  public InvalidNullValueException(Throwable cause) {
    super(cause);
  }

}
