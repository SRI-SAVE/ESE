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

// $Id: ICalDateTime.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.common;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ICalDateTime
        implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final TimeZone GMT_TZ = TimeZone.getTimeZone("GMT");
    private static final TimeZone DEFAULT_TZ = TimeZone.getDefault();
    private Calendar calendar;
    private TimeZone timeZone;

    public ICalDateTime(int year, int month, int date) {
        calendar = Calendar.getInstance();
        calendar.set(year, month, date);
    }

    public ICalDateTime(int year, int month, int date, int hrs, int min, int sec) {
        calendar = Calendar.getInstance();
        calendar.set(year, month, date, hrs, min, sec);
    }

    /**
     * Return an instance for NOW
     */
    public ICalDateTime() {
        calendar = Calendar.getInstance();
        timeZone = DEFAULT_TZ;
    }

    public ICalDateTime(Calendar date, TimeZone tz) {
        calendar = date;
        timeZone = tz == null ? DEFAULT_TZ : tz;
    }

    public ICalDateTime(String value) throws ICalException {
        this(value, null);
    }

    public ICalDateTime(String value, TimeZone tzOverride) throws ICalException {
        TimeZone tz = null;

        if (value.startsWith(";")) {

            int semiIdx = 1;
            String timeZoneId = null;

            while (value.charAt(0) != ':') {
                String valueUC = value.toUpperCase();
                int colIdx = value.indexOf(':');
                semiIdx = value.indexOf(';', 8);
                int endIdx = (colIdx > 0) ? colIdx : semiIdx;

                if (valueUC.startsWith(";TZID=")) {
                    // ";TZID=US-Eastern:19980119T020000"

                    timeZoneId = value.substring(6, endIdx);
                    tz = TimeZone.getTimeZone(timeZoneId);
                    // System.out.println(tz.getID());

                    // System.err.println("value=" + value);

                } else if (valueUC.startsWith(";VALUE=")) {
                    //String valueParam = value.substring(7, endIdx);

                }
                value = value.substring(endIdx);
                break;
            }

        }
        if (value.charAt(0) == ':')
            value = value.substring(1);

        // STAGE 2: process the actual time value
        int length = value.length();
        if (value.charAt(length - 1) == 'Z') {
            tz = GMT_TZ;
        }

        int year = -1, month = -1, day = -1, hour = -1, minute = -1, second = -1;

        int calFormat = -1;
        if (length >= 6) {
            year = Integer.parseInt(value.substring(0, 4));
            month = Integer.parseInt(value.substring(4, 6)) - 1;
            calFormat = 6;
            if (length >= 8) {
                day = Integer.parseInt(value.substring(6, 8));
                calFormat = 8;
                if (length >= 13) {
                    hour = Integer.parseInt(value.substring(9, 11));
                    minute = Integer.parseInt(value.substring(11, 13));
                    calFormat = 13;
                    if (length >= 15) {
                        calFormat = 15;
                        second = Integer.parseInt(value.substring(13, 15));
                    }
                }
            }

        } else {
            throw new ICalException("unknown ical date format: " + value);
        }
        calendar = (tz != null) ? Calendar.getInstance(tz) : Calendar.getInstance();
        switch (calFormat) {
            case 6:
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                break;
            case 8:
                calendar.set(year, month, day, 0, 0, 0);
                break;
            case 13:
                calendar.set(year, month, day, hour, minute, 0);
                break;
            case 15:
                calendar.set(year, month, day, hour, minute, second);
                break;
            default:
                throw new ICalException("unknown cal format " + calFormat);
        }
        // commit the calendar's fields
        calendar.getTimeInMillis();
        if (tzOverride != null) {
            calendar.setTimeZone(tzOverride);
            timeZone = tzOverride;
        } else
            timeZone = (tz == null) ?  DEFAULT_TZ : tz;
    }

    public Calendar getDate() {
        return calendar;
    }

    public static ICalDateTime getCurrentTime() {
      return new ICalDateTime();
    }


    public ICalDateTime add(ICalDuration duration) {
        Date date = new Date(getDate().getTimeInMillis()
                + duration.getTotalMillis());
        Calendar cal = timeZone != null ? Calendar.getInstance(timeZone)
                : Calendar.getInstance();
        cal.setTime(date);
        return new ICalDateTime(cal, timeZone);
    }

    /**
     * Subtract the duration sym this date and return the result. This is a
     * convenience implementation, as durations can be negative.
     *
     * @param duration Duration to subtract sym this time
     * @return Result of the duration subtracted sym this time.
     */
    public ICalDateTime subtract(ICalDuration duration) {
        Date date = new Date(getDate().getTimeInMillis()
                - duration.getTotalMillis());
        Calendar cal = timeZone != null ? Calendar.getInstance(timeZone)
                : Calendar.getInstance();
        cal.setTime(date);
        return new ICalDateTime(cal, timeZone);
    }

    /**
     * subtract an ical date sym this date and return the difference as an
     * ICalDuration
     *
     * @param date
     * @return difference of two dates as an ICalDuration
     */
    public ICalDuration subtract(ICalDateTime date) {
        long thisDate = getDate().getTimeInMillis();
        long thatDate = date.getDate().getTimeInMillis();
        return new ICalDuration(thisDate - thatDate);
    }

 //    public String toString() {
//         SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
//         Date date = calendar.getTime();
//         return format.format(date);
//     }

      public String toString() {
        String format = "yyyyMMdd'T'HHmmss";

        switch (printFormat) {
        case 6:
            format = "yyyyMM";
            break;
        case 8:
            format = "yyyyMMdd";
            break;
        case 13:
            format = "yyyyMMdd'T'HHmm";
            break;
        case 15:
            format = "yyyyMMdd'T'HHmmss";
            break;
        }
        String prefix = "";

        if (timeZone != null && timeZone.equals(GMT_TZ)) {
            format = format + "\'Z\'";
        } else if (timeZone != null) {
            prefix = ";TZID=" + timeZone.getID() + ':';
        }
        if (valueParam != null) {
            // trim off colon
            if (prefix.length() > 0)
                prefix = prefix.substring(0, prefix.length() - 1);
            prefix = prefix + ";VALUE=" + valueParam + ':';
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        if (timeZone != null) {
            sdf.setTimeZone(timeZone);
        }

        return prefix + sdf.format(calendar.getTime());
    }


  /////////////////////////////////////////////////////////////////////////////
  // Speculative additions from LAPDOG
  /////////////////////////////////////////////////////////////////////////////

     /**
     * field that, when set, allows the toString() method to preserve the
     * original formatting of a parsed ICalDateTime. This allows
     * ICalDateTime.parse(string).toString().equals(string). The value of
     * printFormat is the length of the original format, which is enough to
     * derive the encoding. see parse() and toString() for more on how this is
     * encoded/set
     */
    private int printFormat = 0;

    /**
     * preserve the "VALUE" field from parsing.
     */
    private String valueParam;

    /**
     * timezone will be set using the calendar's timezone
     *
     * @param date
     */
    public ICalDateTime(Calendar date) {
        calendar = date;
        timeZone = null;
    }

   /**
     * internal constructor for parse() that takes in an additional printFormat
     * that allows ICalDate to preserve the original date formatting when
     * {@link #toString()} is called.
     *
     * @param date
     * @param printFormat
     * @param tz
     * @param valParam
     */
    private ICalDateTime(Calendar date, int printFormat, TimeZone tz, String valParam) {
        calendar = date;
        this.printFormat = printFormat;
        timeZone = tz;
        valueParam = valParam;
    }

   /**
     * Compares two dates for temporal equality
     * !!! TODO confirm timezone handling is sensible.
     *
     * @param obj  Any Object
     * @return true if the dates refer to the same point in time, else false
     */
    public boolean equals(Object obj) {
      if (!(obj instanceof ICalDateTime)) return false;

      if (obj == this) return true;

      ICalDateTime date = (ICalDateTime) obj;
      long thisDate = getDate().getTimeInMillis();
      long thatDate = date.getDate().getTimeInMillis();

      //FIXME tomlee See note in getCurrentTime()
      // fractions of a second may differ (not sure why), so just truncate since we're
      // only measuring to seconds anyway
      thisDate = thisDate/1000;
      thatDate = thatDate/1000;

      //----log.warn("ICalDateTime equals() " + this + "?=" + date + ". " + thisDate + "?=" + thatDate);
      return (thisDate == thatDate);
    }

    public int compareTo(Object obj) {
      if (this == obj)
        return 0;
      return getDate().compareTo( ((ICalDateTime) obj).getDate() );
    }

    public int hashCode() {
      return toString().hashCode();
    }

   /**
     * @param value
     *            iCal formatted date string
     * @param convertToLocalTime
     * @return an ICalDateTime object instance representing the ical formatted date
     *         string
     */
    public static ICalDateTime parse(String value, boolean convertToLocalTime) {
        return parse(value, convertToLocalTime ? DEFAULT_TZ : null);
    }

    /**
     * @param value
     *            iCal formatted date string
     * @return an ICalDateTime object instance representing the ical formatted date
     *         string
     */
    public static ICalDateTime parse(String value) {
        return parse(value, null);
    }

    /**
     * @param value
     *            iCal formatted date string
     * @param tzOverride
     *            TimeZone to convert date representation into
     * @return an ICalDateTime object instance representing the ical formatted date
     *         string
     */
    public static ICalDateTime parse(String value, TimeZone tzOverride) {
        // I tried an implementation using SimpleDateFormat, but it was too
        // klunky given SDF's stupidities

        // STAGE 1: retrieve optional parameters
        TimeZone tz = null;
        String valueParam = null;
        if (value.startsWith(";")) {

            int semiIdx = 1;
            String timeZoneId = null;

            while (value.charAt(0) != ':') {
                String valueUC = value.toUpperCase();
                int colIdx = value.indexOf(':');
                semiIdx = value.indexOf(';', 8);
                int endIdx = (colIdx > 0) ? colIdx : semiIdx;

                if (valueUC.startsWith(";TZID=")) {
                    // ";TZID=US-Eastern:19980119T020000"

                    timeZoneId = value.substring(6, endIdx);
                    tz = TimeZone.getTimeZone(timeZoneId);
                    // System.out.println(tz.getID());

                    // System.err.println("value=" + value);

                } else if (valueUC.startsWith(";VALUE=")) {
                    valueParam = value.substring(7, endIdx);

                }
                value = value.substring(endIdx);
                break;
            }

        }
        if (value.charAt(0) == ':')
            value = value.substring(1);

        // STAGE 2: process the actual time value
        int length = value.length();
        if (value.charAt(length - 1) == 'Z') {
            tz = GMT_TZ;
        }

        int year = -1, month = -1, day = -1, hour = -1, minute = -1, second = -1;

        int calFormat = -1;
        if (length >= 6) {
            year = Integer.parseInt(value.substring(0, 4));
            month = Integer.parseInt(value.substring(4, 6)) - 1;
            calFormat = 6;
            if (length >= 8) {
                day = Integer.parseInt(value.substring(6, 8));
                calFormat = 8;
                if (length >= 13) {
                    hour = Integer.parseInt(value.substring(9, 11));
                    minute = Integer.parseInt(value.substring(11, 13));
                    calFormat = 13;
                    if (length >= 15) {
                        calFormat = 15;
                        second = Integer.parseInt(value.substring(13, 15));
                    }
                }
            }

        } else {
//             System.err.println("unknown ical date format: " + value);
            return null;
        }
        Calendar cal = (tz != null) ? Calendar.getInstance(tz) : Calendar
                .getInstance();
        switch (calFormat) {
        case 6:
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            break;
        case 8:
            cal.set(year, month, day, 0, 0, 0);
            break;
        case 13:
            cal.set(year, month, day, hour, minute, 0);
            break;
        case 15:
            cal.set(year, month, day, hour, minute, second);
            break;
        default:
//             System.err.println("unknown cal format " + calFormat);
            return null;
        }
        // commit the calendar's fields
        cal.getTimeInMillis();
        if (tzOverride != null) {
            // for computing offset, set it to something non-null
            if (tz == null)
                tz = DEFAULT_TZ;

            cal.setTimeZone(tzOverride);

            // set the appropriate time zone to pass to the ICalDateTime
            // constructor.
            // note that we are using a direct memory comparison
            // intentionally to test whether or not we were called
            // by the parse(date, convertToLocal) method signature
            tz = (tzOverride != DEFAULT_TZ) ? tzOverride : null;
        }

        return new ICalDateTime(cal, calFormat, tz, valueParam);
    }
}
