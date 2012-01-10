
package org.janelia.it.jacs.model.metadata;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 5, 2006
 * Time: 3:36:21 PM
 * It would seem that the location values taken for the GOS project were in the ddd.mm.mmm format.
 */
public class GeoPoint extends CollectionSite implements IsSerializable {

    private String Country = "";
    private String latitude = "";
    private String longitude = "";
    private String altitude = "";
    private String depth = "";

    public GeoPoint() {
    }

    public GeoPoint(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = null;
        this.altitude = null;
    }

    public String getLatitude() {
        return latitude;
    }

    public Double getLatitudeAsDouble() {
        return convertLocationToDouble(latitude);
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = convertDecimalLatitudeToString(latitude);
    }

    public String getLongitude() {
        return longitude;
    }

    public Double getLongitudeAsDouble() {
        return convertLocationToDouble(longitude);
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = convertDecimalLongitudeToString(longitude);
    }

    public String getFormattedLatitude() {
        return latitude.replace('d', '\u00B0');
    }

    public String getFormattedLongitude() {
        return longitude.replace('d', '\u00B0');
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    private static Double convertLocationToDouble(String location) {
        if (location == null || location.trim().length() == 0) {
            return null;
        }
        double dLongtitude = 0.0;
        int index;
        int length;
        int beginNumberIndex;
        String work;
        work = location.trim();

        length = work.length();
        index = work.indexOf('d');
        if (index != -1) {
            // skip all non-digits
            for (beginNumberIndex = 0; beginNumberIndex < length; beginNumberIndex++) {
                if (Character.isDigit(work.charAt(beginNumberIndex))) {
                    break;
                }
            }
            dLongtitude = Double.parseDouble(work.substring(beginNumberIndex, index));
            work = work.substring(index + 1);
            length = length - index;
        }
        index = work.indexOf("'");
        if (index != -1) {
            // skip all non-digits
            for (beginNumberIndex = 0; beginNumberIndex < length; beginNumberIndex++) {
                if (Character.isDigit(work.charAt(beginNumberIndex))) {
                    break;
                }
            }
            dLongtitude = dLongtitude + Double.parseDouble(work.substring(beginNumberIndex, index)) / 60.0;
            work = work.substring(index + 1);
            length = length - index;
        }
        index = work.indexOf("\"");
        if (index != -1) {
            // skip all non-digits
            for (beginNumberIndex = 0; beginNumberIndex < length; beginNumberIndex++) {
                if (Character.isDigit(work.charAt(beginNumberIndex))) {
                    break;
                }
            }
            dLongtitude = dLongtitude + (Double.parseDouble(work.substring(beginNumberIndex, index)) / 60.0) / 60.0;
            work = work.substring(index + 1).trim();
//            length = length - index;
        }
        if (work.toUpperCase().indexOf('W') != -1 || work.toUpperCase().indexOf('S') != -1) {
            dLongtitude = -dLongtitude;
        }
        return dLongtitude;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }

    static String convertDecimalLongitudeToString(double longitude) {
        String lonStr;
        lonStr = decimalToDMS((longitude < 0.0) ? -longitude : longitude);
        lonStr += (longitude < 0.0) ? "W" : "E";
        return lonStr;
    }

    static String convertDecimalLatitudeToString(double latitude) {
        String latStr;
        latStr = decimalToDMS((latitude < 0.0) ? -latitude : latitude);
        latStr += (latitude < 0.0) ? "S" : "N";
        return latStr;
    }

    static String decimalToDMS(double direction) {
        String dms;

        int whole_degree;
        int whole_minutes;

        whole_degree = (int) Math.floor(direction);
        double direction_fraction = direction - (double) whole_degree;
        double minutes_double = direction_fraction * 60.0;
        whole_minutes = (int) (Math.floor(minutes_double));
        double seconds_fraction = minutes_double - (double) whole_minutes;
        Double seconds_double = seconds_fraction * 60.0;

        // format ths seconds string.  we're not allowed to use NumberFormat class
        String sec_string = seconds_double.toString();
        String formatted_sec_string = "";
        int prec = 4;
        boolean dec_found = false;
        int num_digits_right_of_dec = 0;
        for (int pos = 0; pos < sec_string.length() && num_digits_right_of_dec < prec; pos++) {
            if (dec_found) {
                num_digits_right_of_dec++;
            }
            if (sec_string.charAt(pos) == '.') {
                dec_found = true;
            }
            formatted_sec_string += sec_string.charAt(pos);
        }


        dms = whole_degree + "d" + whole_minutes + "'" + formatted_sec_string + "\"";

        return dms;
    }
}
