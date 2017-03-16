package de.aitools.aq.geolocating.wikipedia;

import de.aitools.aq.geolocating.collector.Geolocations;

public class WikiGeolocations extends Geolocations {
  
  private boolean isReverted;
  
  public WikiGeolocations() {
    super();
    this.isReverted = false;
  }
  
  public void setIsReverted(final boolean isReverted) {
    this.isReverted = isReverted;
  }
  
  public boolean getIsReverted() {
    return this.isReverted;
  }
  
}
