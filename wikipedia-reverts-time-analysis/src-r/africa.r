library(maptools)
map <- readShapeSpatial("resources/maps/countries.shp")
countries.africa <- map[grepl(".*Africa.*", map$WBREGION),]$ISO2
table <- read.table("data/enwiki-20160501-results/by-country.txt", stringsAsFactors=FALSE, na.strings="<NA>")
table.africa <- table[table[,1] %in% countries.africa,]

edits <- sum(table[,2])
edits.africa <- sum(table.africa[,2])
print(edits)
print(edits.africa) # 976,709
print(edits.africa/edits)

