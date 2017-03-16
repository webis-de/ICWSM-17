package de.aitools.ie.geolocating;

import java.time.ZonedDateTime;

import de.aitools.aq.geolocating.maps.TimeZones;
import de.aitools.aq.geolocating.maps.TimeZones.TimeZone;

public enum Season {
  WINTER,
  SPRING,
  SUMMER,
  FALL;
  
  /**
   * Uses the following mapping
   * <table>
   * <tr><th>Month</th><th>Northern Hemisphere</th><th>Southern Hemisphere</th></tr>
   * <tr><td>Jan</td><td>Winter</td><td>Summer</td></tr>
   * <tr><td>Feb</td><td>Winter</td><td>Summer</td></tr>
   * <tr><td>Mar</td><td>Spring</td><td>Fall</td></tr>
   * <tr><td>Apr</td><td>Spring</td><td>Fall</td></tr>
   * <tr><td>May</td><td>Spring</td><td>Fall</td></tr>
   * <tr><td>Jun</td><td>Summer</td><td>Winter</td></tr>
   * <tr><td>Jul</td><td>Summer</td><td>Winter</td></tr>
   * <tr><td>Aug</td><td>Summer</td><td>Winter</td></tr>
   * <tr><td>Sep</td><td>Fall</td><td>Spring</td></tr>
   * <tr><td>Oct</td><td>Fall</td><td>Spring</td></tr>
   * <tr><td>Nov</td><td>Fall</td><td>Spring</td></tr>
   * <tr><td>Dec</td><td>Winter</td><td>Summer</td></tr>
   * </table>
   * And the hemisphere is determined by the latitude of the time zone
   * coordinates.
   */
  public static Season get(final ZonedDateTime time) {
    int season = time.getMonthValue() / 3;
    final TimeZone timeZone = TimeZones.forId(time.getZone());
    final double latitude = timeZone.getCoordinates().y;
    if (latitude < 0) { season += 2; }
    if (season > 3) { season -= 4; }
    
    switch (season) {
      case 0:
        return WINTER;
      case 1:
        return SPRING;
      case 2:
        return SUMMER;
      case 3:
        return FALL;
      default:
        throw new RuntimeException("Should not be possible!");
    }
  }

}
