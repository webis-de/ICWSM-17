args.all <- commandArgs(trailingOnly=FALSE)
source(paste(dirname(sub("--file=", "", args.all[grep("--file=", args.all)])), "by-hour-tools.r", sep="/"))

args <- commandArgs(trailingOnly=TRUE)
label <- args[1]
table <- read.table(args[2])
property <- args[3]
country <- args[4]
wiki <- args[5]
vandalism.threshold <- as.numeric(args[6])
output.filename <- args[7]

pdf.prepare(output.filename)
plot.by.hour(table, property, country, wiki, vandalism.threshold, label=label)
dev.off()

