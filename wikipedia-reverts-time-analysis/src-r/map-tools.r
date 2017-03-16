library(maptools)

pdf.cex <- 1.1
pdf.lwd <- 0.1

world.layout.widths <- c(9.5, 1)
world.layout.height <- 3.7

world.xlim <- c(-170, 170)
world.ylim <- c(-50, 80)
us.xlim <- c(-125,-67)
us.ylim <- c(24, 49)
europe.xlim <- c(-10, 30)
europe.ylim <- c(37, 72)

inlay.indian.user <- c(50, 110, -55, -2.5) # xmin xmax ymin ymax
inlay.pacific.user <- c(-180, -80, -55, -11.9) # xmin xmax ymin ymax

resolve.path <- function(path) {
  if (!exists("srcr")) {
    args.all <- commandArgs(trailingOnly=FALSE)
    srcr <- dirname(sub("--file=", "", args.all[grep("--file=", args.all)]))
  }
  return(paste(srcr, path, sep="/"))
}

########################################
# VALUES
########################################

values.merge <- function(vals, keys) {
  for (key in keys) {
    if (!key %in% vals$V1) {
      vals <- rbind(vals, data.frame(V1 = key, V2 = 0, V3 = 0, V4 = 0))
    }
  }
  indices <- vals$V1 %in% keys
  vals[indices, 2] <- sum(vals[indices, 2])
  vals[indices, 3] <- sum(vals[indices, 3])
  return(vals)
}

values.merge.microsoft <- function(vals) {
  # Pacific
  #vals <- values.merge(vals, c("America/Los_Angeles"))
  # Mountain
  vals <- values.merge(vals, c("America/Boise", "America/Denver", "America/Phoenix"))
  # Central
  vals <- values.merge(vals, c("America/Chicago", "America/Menominee", "America/North_Dakota/Beulah", "America/North_Dakota/Center", "America/North_Dakota/New_Salem"))
  # Eastern
  vals <- values.merge(vals, c("America/Detroit", "America/Indiana/Indianapolis", "America/Indiana/Knox", "America/Indiana/Marengo", "America/Indiana/Petersburg", "America/Indiana/Tell_City", "America/Indiana/Vevay", "America/Indiana/Vincennes", "America/Indiana/Winamac", "America/Kentucky/Louisville", "America/New_York", "America/Kentucky/Monticello"))
  return(vals)
}

########################################
# READING MAPS
########################################

read.map.timezones <- function() {
  print("Reading map: time zones")
  return(readShapeSpatial(resolve.path("../resources/maps/timezones.shp")))
}

filter.map.timezones.us <- function(map) {
  map <- map[
            map$TZID == "America/Boise" |
            map$TZID == "America/Chicago" |
            map$TZID == "America/Denver" |
            map$TZID == "America/Detroit" |
            map$TZID == "America/Indiana/Indianapolis" |
            map$TZID == "America/Indiana/Knox" |
            map$TZID == "America/Indiana/Marengo" |
            map$TZID == "America/Indiana/Petersburg" |
            map$TZID == "America/Indiana/Tell_City" |
            map$TZID == "America/Indiana/Vevay" |
            map$TZID == "America/Indiana/Vincennes" |
            map$TZID == "America/Indiana/Winamac" |
            map$TZID == "America/Kentucky/Louisville" |
            map$TZID == "America/Kentucky/Monticello" |
            map$TZID == "America/Los_Angeles" |
            map$TZID == "America/Menominee" |
            map$TZID == "America/New_York" |
            map$TZID == "America/North_Dakota/Beulah" |
            map$TZID == "America/North_Dakota/Center" |
            map$TZID == "America/North_Dakota/New_Salem" |
            map$TZID == "America/Phoenix"
         ,]
  return(map)
}

read.map.countries <- function() {
  print("Reading map: countries")
  return(readShapeSpatial(resolve.path("../resources/maps/countries.shp")))
}

filter.map.countries.us <- function(map) {
  return(map[!is.na(map$ISO2) & map$ISO2 == "US",])
}

filter.map.countries.europe <- function(map) {
  return(map[grepl("Europe", map$UNREGION1),])
}

read.map.states <- function() {
  print("Reading map: states")
  return(readShapeSpatial(resolve.path("../resources/maps/states.shp")))
}

########################################
# COLORS
########################################

palette.absolute.make <- function(values.max, log.base=10) {
  absolute.upper.bound <- ceiling(log(values.max, base=log.base))
  absolute.lower.bound <- 0
  absolute.palette.length <- absolute.upper.bound - absolute.lower.bound
  return(colorRampPalette(c("#ffffff", "#feb24c", "#f03b20"))(absolute.palette.length))
}

palette.ratio.make <- function(length) {
  return(colorRampPalette(c("#ffeda0", "#feb24c", "#f03b20"))(length))
}

values.max.default <- 99999999
lim.ratio.default <- c(0.1, 0.3)
palette.absolute.default <- palette.absolute.make(values.max.default)
palette.ratio.default <- palette.ratio.make(10)

color.absolute.pick <- function(value, palette = palette.absolute.default, log.base=10) {
  l <- log(value, base=log.base) + 1
  return(palette[l])
}

colors.absolute.pick <- function(keys, values, palette = palette.absolute.default) {
  colors <- rep("#ffffff", length(keys))
  for (row in 1:dim(values)[1]) {
    colors[!is.na(keys) & keys == values[row,1]] <- color.absolute.pick(values[row,2], palette)
  }
  return(colors)
}

color.ratio.pick <- function(value, palette = palette.ratio.default, lim = lim.ratio.default) {
  l <- min(length(palette), max(1, ceiling((value - lim[1]) / (lim[2] - lim[1]) * length(palette))))
  return(palette[l])
}

colors.ratio.pick <- function(keys, values, reverted.threshold, palette = palette.ratio.default, lim = lim.ratio.default, color.below.threshold = "#ffffff") {
  colors <- rep(color.below.threshold, length(keys))
  for (row in 1:dim(values)[1]) {
    if (values[row,3] >= reverted.threshold) {
      colors[!is.na(keys) & keys == values[row,1]] <- color.ratio.pick(values[row,3]/values[row,2], palette, lim)
    }
  }
  return(colors)
}

########################################
# PLOTTING
########################################

plot.inlay.fig.get <- function(inlay) {
  return(c(grconvertX(inlay[1:2], from="user", to="ndc"), grconvertY(inlay[3:4], from="user", to="ndc")))
}

plot.prepare <- function(overlay = FALSE, cex = pdf.cex, boxed = FALSE, fig = NULL, mar.right = 0) {
  mar = c(0,0,0,mar.right)
  oma = rep(0,4)

  if (is.null(fig)) {
    par(new = overlay, mar = mar, oma = oma)
  } else {
    par(new = overlay, mar = mar, oma = oma, fig=fig)
  }

  if (boxed) {
    frame()
    rect(par("usr")[1], par("usr")[3], par("usr")[2], par("usr")[4], col = "white")
    par(new = TRUE)
  }
}

plot.ratio.prepare <- function(palette = palette.ratio.default, lim = lim.ratio.default, cex = pdf.cex, layout.widths = world.layout.widths) {
  layout(t(c(2,1)), widths=layout.widths)
  plot.prepare(mar.right = 3)
  print("Plotting key")
  lower.bound <- lim[1]
  upper.bound <- lim[2]
  step <- (upper.bound - lower.bound) / length(palette)
  image(1, 1:length(palette), t(1:length(palette)), xlab="", ylab="", axes=FALSE, col=palette)
  tics <- (1:(length(palette)-1))
  axis(4, at=tics+0.5, label=FALSE)
  axis(4, at=tics+0.5, lwd=0, lwd.ticks=0, label=sprintf("%.2f", (((lower.bound / step)+1):((upper.bound / step)-1))*step), cex.axis = cex, las=1, line=-0.25)
  tics <- (0:length(palette))
  axis(4, at=tics+0.5, label=FALSE, lwd.ticks=0)
  plot.prepare(overlay = TRUE)
}

plot.absolute.prepare <- function(palette = palette.absolute.default, values.max = values.max.default, cex = pdf.cex, layout.widths = world.layout.widths, log.base=10) {
  layout(t(c(2,1)), widths=layout.widths)
  plot.prepare(mar.right = 3)
  print("Plotting key")
  absolute.upper.bound <- ceiling(log(values.max, base=log.base))
  absolute.lower.bound <- 0
  image(1, 1:length(palette), t(1:length(palette)), xlab="", ylab="", axes=FALSE, col=palette)
  tics <- (1:(length(palette)-1))
  axis(4, at=tics+0.5, label=log.base^((absolute.lower.bound+1):(absolute.upper.bound-1)), cex.axis = cex)
  tics <- (0:length(palette))
  axis(4, at=tics+0.5, label=FALSE, lwd.ticks=0)
  plot.prepare(overlay = TRUE)
}

plot.inlay.prepare <- function(fig, cex = pdf.cex) {
  plot.prepare(overlay = TRUE, cex = cex, boxed = TRUE, fig = fig)
}

plot.map <- function(map.colors, col, map.border.dark = NULL, map.border.light = NULL, xlim = bbox(map.colors)[1,], ylim = bbox(map.colors)[2,], lwd = pdf.lwd) {
  print("Plotting colors")
  if (is.logical(map.border.dark)) {
    plot(map.colors, col = col, xlim = xlim, ylim = ylim, border=map.border.dark, lwd = lwd)
  } else {
    plot(map.colors, col = col, xlim = xlim, ylim = ylim, border=FALSE)
  }

  if (!is.null(map.border.light)) {
    par(new = TRUE)
    print("Plotting light borders")
    plot(map.border.light, xlim = xlim, ylim = ylim, lwd = lwd, border="grey")
  }

  if (!is.logical(map.border.dark) & !is.null(map.border.dark)) {
    par(new = TRUE)
    print("Plotting dark borders")
    plot(map.border.dark, xlim = xlim, ylim = ylim, lwd = lwd, border="black")
  }
}

plot.label <- function(label, cex = pdf.cex) {
  mtext(label, side=1, line=0.75, cex=cex)
  par(new = TRUE)
}

