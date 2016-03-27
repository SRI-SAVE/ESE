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

// $Id: ICalDuration.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

import java.io.Serializable;

public class ICalDuration
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final long SECONDS_MS = 1000;

    private static final long MINUTES_MS = SECONDS_MS * 60;

    private static final long HOURS_MS = MINUTES_MS * 60;

    private static final long DAYS_MS = HOURS_MS * 24;

    private static final long WEEKS_MS = DAYS_MS * 7;

    /*
       *  Value Name: DURATION

          Purpose: This value type is used to identify properties that contain a duration of time.

          Formal Definition: The value type is defined by the following notation:

           dur-value  = (["+"] / "-") "P" (dur-date / dur-time / dur-week)

           dur-date   = dur-day [dur-time]
           dur-time   = "T" (dur-hour / dur-minute / dur-second)
           dur-week   = 1*DIGIT "W"
           dur-hour   = 1*DIGIT "H" [dur-minute]
           dur-minute = 1*DIGIT "M" [dur-second]
           dur-second = 1*DIGIT "S"
           dur-day    = 1*DIGIT "D"

          Description: If the property permits, multiple "duration" values are specified by a COMMA
          character (US-ASCII decimal 44) separated list of values. The format is expressed as the
          [ISO 8601] basic format for the duration of time. The format can represent durations in
          terms of weeks, days, hours, minutes, and seconds.
       */

    int[] values;

    static final int WEEK = 4;
    static final int DAY = 0;
    static final int HOUR = 1;
    static final int MINUTE = 2;
    static final int SECOND = 3;

    int totalmin;

    boolean isPositive;

    public ICalDuration(int w, int d, int h, int m, int s, boolean isPositive) {
        set(w, d, h, m, s, isPositive);
    }

    public final void set(
            int w,
            int d,
            int h,
            int m,
            int s,
            boolean isPositive) {
        values = new int[5];
        values[DAY] = d;
        values[HOUR] = h;
        values[MINUTE] = m;
        values[SECOND] = s;
        values[WEEK] = w;

        this.isPositive = isPositive;
        initTotalMin();
    }

    /**
     * @param millis
     */
    public ICalDuration(long millis) {
        isPositive = (millis >= 0);
        //have to reverse the sign to positive in order to keep
        //the Math.floor() logic happy
        if (!isPositive) {
            millis = -millis;
        }
        values = new int[5];
        values[WEEK] = (int) (millis / WEEKS_MS);
        millis -= values[WEEK] * WEEKS_MS;
        values[DAY] = (int) (millis / DAYS_MS);
        millis -= values[DAY] * DAYS_MS;
        values[HOUR] = (int) (millis / HOURS_MS);
        millis -= values[HOUR] * HOURS_MS;
        values[MINUTE] = (int) (millis / MINUTES_MS);
        millis -= values[MINUTE] * MINUTES_MS;
        values[SECOND] = (int) (millis / SECONDS_MS);

        initTotalMin();
    }

    private void initTotalMin() {
        totalmin =
                (((values[WEEK] * 7 * 24) + (values[DAY] * 24) + values[HOUR])
                        * 60)
                        + values[MINUTE]
                        + Math.round((float) values[SECOND] / 60);
        if (!isPositive)
            totalmin *= -1;
    }

    /**
     * add two ical durations
     *
     * @param dur ICalDuration to add to this duration
     * @return new ICalDuration object with the sum of the
     *         this duration and the argument
     */
    public ICalDuration add(ICalDuration dur) {
        //this implementation has the disadvantage that it
        //doesn't try to preserve the units of the original
        //durations.  i.e. if you add 100 seconds to 100 seconds,
        //you will get 3 minutes and 20 seconds instead of 200 seconds
        //as the duration.  however, considering that durations
        //may be negative, this is the least complicated way of
        //doing this
        long millis = getTotalMillis() + dur.getTotalMillis();
        return new ICalDuration(millis);
    }

    /**
     * subtract the specified duration sym this duration.
     * This is a convenience form of add(), as durations can
     * be negative.
     *
     * @param dur ICalDuration to subtract sym this duration
     * @return new ICalDuration object with the difference of the
     *         this duration and the argument
     */
    public ICalDuration subtract(ICalDuration dur) {
        long millis = getTotalMillis() - dur.getTotalMillis();
        return new ICalDuration(millis);
    }

    /**
     * @return true if duration is specified as a positive duration (e.g. "P7W"), not
     *         a negative duration (e.g. "-P7W")
     */
    public boolean isPositive() {
        return isPositive;
    }

    /**
     * convert duration to String.  iCal format is used to encode
     * duration values
     *
     * @return iCal-encoded duration
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buff = new StringBuffer();
        if (!isPositive)
            buff.append("-");
        buff.append('P');
        boolean isZero = true;
        boolean removeWeeks =
                (values[WEEK] > 0
                        && (values[DAY] > 0
                        || values[HOUR] > 0
                        || values[MINUTE] > 0
                        || values[SECOND] > 0));

        if (values[WEEK] > 0 && !removeWeeks) {
            isZero = false;
            buff.append(values[WEEK]).append('W');
        }
        if (values[DAY] > 0 || removeWeeks) {
            isZero = false;
            int days = values[DAY];
            if (removeWeeks)
                days += values[WEEK] * 7;
            buff.append(days).append('D');
        }
        if (values[HOUR] > 0 || (values[MINUTE] > 0) || (values[SECOND] > 0)) {
            isZero = false;
            buff.append('T');
            if (values[HOUR] > 0)
                buff.append(values[HOUR]).append('H');
            if (values[MINUTE] > 0 || (values[SECOND] > 0 && values[HOUR] > 0))
                buff.append(values[MINUTE]).append('M');
            if (values[SECOND] > 0)
                buff.append(values[SECOND]).append('S');
        }
        if (isZero)
            return "PT0S";
        return buff.toString();
    }

    /**
     * @return value of the specified field; -1 if unknown
     */
    public int getValue(int field) {
        return (field >= 0 && field <= values.length) ? values[field] : -1;
    }

    /**
     * get the duration in minutes
     *
     * @return the equivalent total #minutes
     */
    public int getTotalMin() {
        return totalmin;
    }

    /**
     * get the duration in milliseconds
     *
     * @return value of duratio in milliseconds
     */
    public long getTotalMillis() {
        long retVal =
                values[WEEK] * WEEKS_MS
                        + values[DAY] * DAYS_MS
                        + values[HOUR] * HOURS_MS
                        + values[MINUTE] * MINUTES_MS
                        + values[SECOND] * SECONDS_MS;
        return isPositive ? retVal : -1 * retVal;
    }

    /**
     * convert buff containing a positive integer
     * to an int.  buffer is also cleared.
     *
     * @param buff Buffer contain integer value.
     *             Will be CLEARED upon return
     * @return positive integer value of buffer,
     *         or -1 if buffer value is not a valid
     *         positive integer
     */
    private int buffToInt(StringBuffer buff) {
        if (buff == null || buff.length() == 0)
            return -1;
        try {
            int retVal = Integer.parseInt(buff.toString());
            buff.setLength(0); //clear buff
            return retVal;
        } catch (NumberFormatException e) {
            buff.setLength(0); //clear buff
            return -1;
        }
    }

    /**
     * Converts a string in ICal format (PdDThHmMsS) into an IcalDuration.
     *
     * @throws ICalException if durstr has invalid format
     */
    public ICalDuration(String durstr) throws ICalException {
        if (durstr == null || durstr.length() < 2) {
            throw new ICalException("Error in parsing " + durstr);
        }
        //process (["+"] / "-") "P"
        boolean isPositive = true;
        char c = durstr.charAt(0);
        int pPos = 0;
        if (c == '-') {
            isPositive = false;
            pPos = 1;
        } else if (c == '+') {
            pPos = 1;
        }
        int pix = durstr.indexOf('P');
        if (pix != pPos) {
            throw new ICalException("Error in parsing " + durstr);
        }

        //despite what the spec says, people seem to think
        //that it's valid it mix dur-week with dur-date/dur-time, so
        //we will have a liberal parser that allows this, but
        //only before the dur-time declaration

        int w = -1;
        int d = -1;
        int h = -1;
        int m = -1;
        int s = -1;

        StringBuffer buff = new StringBuffer();
        int idx = pPos + 1;
        boolean inDurTime = false; //'T' must proceeed H/M/S
        while (idx < durstr.length()) {
            c = durstr.charAt(idx++);
            if (Character.isDigit(c)) {
                buff.append(c);
            } else if (c == 'W') {
                if (inDurTime || w > -1)
                    throw new ICalException("Error in parsing " + durstr);
                w = buffToInt(buff);
                if (w < 0)
                    throw new ICalException("Error in parsing " + durstr);

            } else if (c == 'D') {
                if (inDurTime || d > -1)
                    throw new ICalException("Error in parsing " + durstr);
                d = buffToInt(buff);
                if (d < 0)
                    throw new ICalException("Error in parsing " + durstr);

            } else if (c == 'T') {
                if (inDurTime || s > -1)
                    throw new ICalException("Error in parsing " + durstr);
                inDurTime = true;

            } else if (c == 'H') {
                if (!inDurTime || s > -1)
                    throw new ICalException("Error in parsing " + durstr);
                h = buffToInt(buff);
                if (h < 0)
                    throw new ICalException("Error in parsing " + durstr);

            } else if (c == 'M') {
                if (!inDurTime || s > -1)
                    throw new ICalException("Error in parsing " + durstr);
                m = buffToInt(buff);
                if (m < 0)
                    throw new ICalException("Error in parsing " + durstr);

            } else if (c == 'S') {
                if (!inDurTime || s > -1)
                    throw new ICalException("Error in parsing " + durstr);
                s = buffToInt(buff);
                if (s < 0)
                    throw new ICalException("Error in parsing " + durstr);
            }
        }

        //convert unparsed values to zero
        if (w < 0)
            w = 0;
        if (d < 0)
            d = 0;
        if (h < 0)
            h = 0;
        if (m < 0)
            m = 0;
        if (s < 0)
            s = 0;

        set(w, d, h, m, s, isPositive);
    }

  /**
     * Converts a string in ICal format (PdDThHmMsS) into an IcalDuration.
     *
     * @return the equivalent IcalDuration or null if string is invalid
   */
    public static ICalDuration parse(String parseMe) {
      try {
        return new ICalDuration(parseMe);
      }
      catch (ICalException ice) {
        return null;
      }
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof ICalDuration) {
            ICalDuration d = (ICalDuration) o;
            return d.getTotalMillis() == getTotalMillis();
        } else
            return false;
        }
        public int hashCode() {
            return getTotalMin() ^ values[SECOND];
        }
    }
