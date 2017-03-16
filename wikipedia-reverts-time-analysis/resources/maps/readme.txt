countries: from GADM 2.8 adm0 \cf{http://www.gadm.org/}
states: from GADM 2.8 adm1 \cf{http://www.gadm.org/}
timezones: from tz_world 2016d \cf{http://efele.net/maps/tz/world/}

Map simplifier: \cf{http://mapshaper.org/}
Sometimes requires to remove NULL objects later on:
R
  library(maptools)
  m <- readShapeSpatial("resources/maps/countries.shp", delete_null_obj=TRUE)
  writeSpatialShape(m, "resources/maps/countries.shp")
