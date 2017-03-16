args <- commandArgs(trailingOnly=TRUE)
data.colors <- read.table(args[1], stringsAsFactors=FALSE)
output.file <- args[2]

pdf(output.file, width=6, height=0.5)
par(mar=rep(0,4), oma=rep(0,4))
plot.new()
legend("center", lty=1, lwd=2, horiz=TRUE, border=TRUE, text.width=0.15, legend=data.colors[,1], col=data.colors[,2])
dev.off()
