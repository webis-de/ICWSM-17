args.all <- commandArgs(trailingOnly=FALSE)
source(paste(dirname(sub("--file=", "", args.all[grep("--file=", args.all)])), "by-hour-tools.r", sep="/"))

args <- commandArgs(trailingOnly=TRUE)
label.offset <- as.numeric(args[1])
labels <- letters[(label.offset+1):(label.offset+10)]
property <- args[2]
vandalism.threshold <- as.numeric(args[3])
output.filename <- args[4]

plot.quarter <- function(label, vertical.half, horizontal.half, arg) {
  table <- read.table(args[arg])
  country <- args[arg+1]
  wiki <- args[arg+2]
  label.complete <- paste("(", label, ")", sep="")
  plot.by.hour(table, property, country, wiki, vandalism.threshold, label=label.complete, vertical.half=vertical.half, horizontal.half=horizontal.half, num.horizontal=2)
}

pdf.cex.main <- 1.2 * pdf.cex.main

pdf(paste(output.filename, "1.pdf", sep=""), width=5, height=4)
layout(matrix(1:4, nrow=2), widths=c(725,600), heights=c(452.5, 497.5))
plot.quarter(labels[ 1], "top", "left", 5)
plot.quarter(labels[ 2], "bottom", "left", 8)
plot.quarter(labels[ 3], "top", "2", 11)
plot.quarter(labels[ 4], "bottom", "2", 14)
dev.off()
pdf(paste(output.filename, "2.pdf", sep=""), width=5, height=4)
layout(matrix(1:4, nrow=2), widths=c(600,600), heights=c(452.5, 497.5))
plot.quarter(labels[ 5], "top", "1", 17)
plot.quarter(labels[ 6], "bottom", "1", 20)
plot.quarter(labels[ 7], "top", "2", 23)
plot.quarter(labels[ 8], "bottom", "2", 26)
dev.off()
pdf(paste(output.filename, "3.pdf", sep=""), width=5, height=4)
layout(matrix(1:4, nrow=2), widths=c(600,695), heights=c(452.5, 497.5))
plot.quarter(labels[ 7], "top", "1", 23)
plot.quarter(labels[ 8], "bottom", "1", 26)
plot.quarter(labels[ 9], "top", "right", 29)
plot.quarter(labels[10], "bottom", "right", 32)
dev.off()

