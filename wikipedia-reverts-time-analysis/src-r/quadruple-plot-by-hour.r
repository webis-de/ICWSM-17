args.all <- commandArgs(trailingOnly=FALSE)
source(paste(dirname(sub("--file=", "", args.all[grep("--file=", args.all)])), "by-hour-tools.r", sep="/"))

args <- commandArgs(trailingOnly=TRUE)
label.offset <- as.numeric(args[1])
labels <- letters[(label.offset+1):(label.offset+4)]
property <- args[2]
vandalism.threshold <- as.numeric(args[3])
output.filename <- args[4]

plot.quarter <- function(label, vertical.half, horizontal.half, arg) {
  table <- read.table(args[arg])
  country <- args[arg+1]
  wiki <- args[arg+2]
  label.complete <- paste("(", label, ")", sep="")
  plot.by.hour(table, property, country, wiki, vandalism.threshold, label=label.complete, vertical.half=vertical.half, horizontal.half=horizontal.half)
}

pdf.cex.main <- 1.2 * pdf.cex.main

pdf.prepare(output.filename)
layout(matrix(c(1,2,3,4), nrow=2), widths=c(725,695), heights=c(452.5, 497.5))
plot.quarter(labels[1], "top", "left", 5)
plot.quarter(labels[2], "bottom", "left", 8)
plot.quarter(labels[3], "top", "right", 11)
plot.quarter(labels[4], "bottom", "right", 14)
dev.off()

