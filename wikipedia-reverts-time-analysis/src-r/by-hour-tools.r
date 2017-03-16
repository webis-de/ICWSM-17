pdf.cex.main <- 1.0
pdf.cex <- pdf.cex.main * 1

max.vandalism.ratio <- 0.5

grey.text <- grey(0.4)

hour.start <- 6
hour.order <- c((hour.start-1):24,1:(hour.start+1)) # offset of 1 since it starts with hour 0:00


resolve.path <- function(path) {
  if (!exists("srcr")) {
    args.all <- commandArgs(trailingOnly=FALSE)
    srcr <- dirname(sub("--file=", "", args.all[grep("--file=", args.all)]))
  }
  return(paste(srcr, path, sep="/"))
}

########################################
# NUMBERS
########################################

get.absolute.max.millions <- function(country, property="") {
  max.millions <- 7
  if (country != "Worldwide") {
    max.millions <- 3
  }
  if (property == "timezone") {
    max.millions <- 3
  }
  return(max.millions)
}

read.colors <- function(property) {
  if (property == "") {
    return("#000000")
  } else {
    return(read.table(resolve.path(paste("../resources/colors/", property, ".txt", sep="")), stringsAsFactors=FALSE))
  }
}


########################################
# DATA
########################################

get.vandalism.offset <- function(vandalism.with.comment) {
  if (vandalism.with.comment) {
    return(1)
  } else {
    return(0)
  }
}

get.vandalism.ratios <- function(table, vandalism.with.comment) {
  vandalism.offset <- get.vandalism.offset(vandalism.with.comment)
  if (dim(table)[2] == 4) {
    return(table[[3 + vandalism.offset]] / table$V2)
  } else {
    return(table[[4 + vandalism.offset]] / table$V3)
  }
}

get.vandalism.ratios.trustworthiness <- function(table, vandalism.with.comment, vandalism.threshold) {
  vandalism.offset <- get.vandalism.offset(vandalism.with.comment)
  if (dim(table)[2] == 4) {
    return(table[[3 + vandalism.offset]] >= vandalism.threshold)
  } else {
    return(table[[4 + vandalism.offset]] >= vandalism.threshold)
  }
}

get.marginalized.table <- function(table) {
  if (dim(table)[2] == 4) {
    return(table)
  } else {
    table.marginalized <- matrix(0,24,3)
    table.marginalized[,1] <- 0:23
    for (h in 0:23) {
      sums <- colSums(data.matrix(table[table$V2 == h, 3:4]))
      table.marginalized[(h+1),2:3] <- sums 
    }
    return(table.marginalized)
  }
}

########################################
# TEXT
########################################

get.text.left.description <- function(property) {
  if (property == "") {
    description <- "Vandalism ratio per hour of day"
  } else {
    description <- paste("Vandalism ratio per", property, "and hour of day")
  }
  return(description)
}

########################################
# COLORS
########################################

tint.value <- function(value) {
  tint.factor <- 0.75
  value.parsed <- strtoi(paste("0x", value, sep="")) / 255
  return(value.parsed + (1 - value.parsed) * tint.factor)
}

tint <- function(col) {
  r <- substr(col,2,3)
  g <- substr(col,4,5)
  b <- substr(col,6,7)
  return(rgb(tint.value(r),tint.value(g),tint.value(b)))
}

########################################
# PLOTTING
########################################

get.plot.scale <- function(partplot = FALSE) {
  if (partplot) {
    return(0.6) # TODO
  } else {
    return(1)
  }
}

pdf.prepare <- function(filename) {
  pdf(filename, width=6, height=4)
}

plot.prepare <- function(vertical.half = FALSE, horizontal.half = FALSE, overlay = FALSE, num.horizontal = 2) {
  mar <- c(2, 2.6, 1, 2)
  mfg <- c(1,1)
  if (!is.logical(horizontal.half)) {
    if (vertical.half == "top") {
      mar[1] = 0
    } else {
      mfg[2] <- 2
      mar[3] = 0
    }
    if (horizontal.half == "left") {
      mar[4] = 0
    } else if (horizontal.half == "right") {
      mfg[1] <- num.horizontal
      mar[2] = 0
    } else {
      mfg[1] <- as.numeric(horizontal.half)
      mar[2] = 0
      mar[4] = 0
    }
  }
  oma = rep(0,4)
  par(new = overlay, mar = mar, oma = oma, mfg=mfg)
}

plot.absolute <- function(table, country, property="", vertical.half = FALSE, horizontal.half = FALSE, overlay = FALSE, num.horizontal = 2) {
  partplot <- !is.logical(vertical.half)
  plot.prepare(vertical.half, horizontal.half, overlay, num.horizontal = num.horizontal)
  max.millions <- get.absolute.max.millions(country, property)
  table.marginalized <- get.marginalized.table(table)
  scale <- get.plot.scale(partplot)

  plot(0:23,(0:23)/23*(max.millions*1000000), xlab="", xaxt="n", ylab="", yaxt="n", type="n")
  if (!partplot | horizontal.half == "right") {
    axis(4, at=c((0:max.millions)*1000000), label=FALSE, tck=-0.015/scale, col.axis=grey.text, cex.axis=pdf.cex)
    axis(4, at=c((0:max.millions)*1000000), label=0:max.millions, lwd=0, line=-0.6, las=1, col.axis=grey.text, cex.axis=pdf.cex)
    mtext(side=4, line=1, "Edits (in millions)", col=grey.text, cex=pdf.cex)
    if (!partplot | vertical.half == "top") {
      at <- grconvertX(1, from="ndc", to="user")
      mtext(side=3, line=0.1, at=at, "Edits per hour of day", adj=1, col=grey.text, cex=pdf.cex)
    }
  }
# Total lines
  lines((-2:24)+0.5, table.marginalized[hour.order,2], col="lightgrey", lty=2, lwd=2)
  lines((-2:24)+0.5, table.marginalized[hour.order,3], col="darkgrey", lty=2, lwd=2)
}

plot.ratio.lines <- function(ratios, ratios.trustworthiness, names=NULL, colors="#000000", lty=1, lwd=2) {
  if (is.null(names)) {
    for (i in 1:(length(hour.order)-1)) {
      p <- mean(ratios[hour.order[i:(i+1)]])
      v <- ratios[hour.order[(i+1)]]
      n <- mean(ratios[hour.order[(i+1):(i+2)]])
      col <- colors
      clty=1
      if (!ratios.trustworthiness[hour.order[(i+1)]]) {
        #col <- tint(colors)
        clty=3
      }
      lines(c(-2,-1.5,-1)+i, c(p,v,n), lwd=lwd, col=col, lty=clty)
    }
  } else {
    for (c in 1:dim(colors)[1]) {
      plot.ratio.lines(ratios[names == colors[c,1]], ratios.trustworthiness[names == colors[c,1]], color=colors[c,2], lty=lty, lwd=lwd)
    }
  }
}

plot.ratio <- function(table, vandalism.threshold, property="", vertical.half = FALSE, horizontal.half = FALSE, overlay = FALSE, num.horizontal = 2) {
  partplot <- !is.logical(vertical.half)
  plot.prepare(vertical.half, horizontal.half, overlay, num.horizontal = num.horizontal)
  ratios <- get.vandalism.ratios(table, FALSE)
  ratios.trustworthiness <- get.vandalism.ratios.trustworthiness(table, FALSE, vandalism.threshold)
  table.marginalized <- get.marginalized.table(table)
  ratio.max <- max.vandalism.ratio
  description <- get.text.left.description(property)
  scale <- get.plot.scale(partplot)

  plot(0:23,(0:23)/23*ratio.max, xlab="", xaxt="n", ylab="", yaxt="n", type="n")
  abline(h=(0:(ratio.max*10))/10, v=(0:11)*2, col="lightgray", lty="dotted")

  if (!partplot | horizontal.half == "left") {
    mtext(side=2, line=1.7, "Vandalism ratio", cex=pdf.cex)
    axis(2, label=FALSE, tck=-0.015/scale, cex.axis=pdf.cex)
    axis(2, lwd=0, line=-0.6, las=1, cex.axis=pdf.cex)
    if (!partplot | vertical.half == "top") {
      at <- grconvertX(0, from="ndc", to="user")
      mtext(side=3, line=0.1, at=at, description, adj=0, cex=pdf.cex, font=2)
    }
  }

  if (!partplot | vertical.half == "bottom") {
    axis(1, at=0:23, label=FALSE, tck=-0.015/scale, cex.axis=pdf.cex)
    if (partplot) {
      axis(1, at=(0:5)*4, label=table.marginalized[hour.order,1][(0:5)*4+3], lwd=0, line=-0.75, las=1, cex.axis=pdf.cex)
    } else {
      axis(1, at=(0:11)*2, label=table.marginalized[hour.order,1][(0:11)*2+3], lwd=0, line=-0.75, las=1, cex.axis=pdf.cex)
    }
    mtext(side=1, line=1, "Hour of day", cex=pdf.cex)
  }

  colors <- read.colors(property)
  names <- NULL
  if (length(colors) > 1) {
    names <- table$V1
  }
  plot.ratio.lines(ratios, ratios.trustworthiness, names, colors)

  if (property == "") {
    ratios <- get.vandalism.ratios(table, TRUE)
    ratios.trustworthiness <- get.vandalism.ratios.trustworthiness(table, TRUE, vandalism.threshold)
    plot.ratio.lines(ratios, ratios.trustworthiness, names, colors, lty=1, lwd=1)
  }
}

plot.labels <- function(wiki, country, vandalism.with.comment, label = "", partplot = FALSE) {
  ratio.max <- max.vandalism.ratio
  if (label != "") {
    text(0, ratio.max, adj=c(0,1), label, cex=pdf.cex.main)
  }
  text(23, ratio.max, adj=c(1,1), paste(wiki, "Wikipedia"), cex=pdf.cex.main)
  factor <- 0.925
  if (partplot) {
    factor <- 0.875
  }
  text(23, ratio.max*factor, adj=c(1,1), paste("from", country), cex=pdf.cex.main)
}

plot.by.hour <- function(table, property, country, wiki, vandalism.threshold, label = "", vertical.half = FALSE, horizontal.half = FALSE, num.horizontal = 2) {
  plot.absolute(table, country, property, vertical.half = vertical.half, horizontal.half = horizontal.half, num.horizontal = num.horizontal)
  plot.ratio(table, vandalism.threshold, property, vertical.half = vertical.half, horizontal.half = horizontal.half, overlay=TRUE, num.horizontal = num.horizontal)
  plot.labels(wiki, country, vandalism.with.comment, label=label, partplot=!is.logical(vertical.half))
}

