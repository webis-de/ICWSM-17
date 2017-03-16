args <- commandArgs(trailingOnly=TRUE)
input <- args[1]
output <- args[2]
cex <- 0.75


data <- read.table(input)
data <- data[data$V2 > 0,]
data.log <- log(data)
model <- lm(data.log$V2 ~ data.log$V1)
a <- model$coefficients[1]
b <- model$coefficients[2]

pdf(output, width=2.5, height=1.5)
par(mar=c(1.5,2,0,0))

#options(scipen=5)
plot(data, ylim=c(1, max(data$V2)), xlab="", ylab="", type="p", log="yx", yaxt="n", xaxt="n")
axis(1, labels=FALSE, tck=-0.03)
axis(1, line=-1, lwd=0, cex.axis=cex)
axis(2, at=c(1, 100, 10000, 1000000), labels=FALSE, tck=-0.03)
axis(2, at=c(1, 100, 10000, 1000000), labels=c(expression(1),expression(10^2),expression(10^4),expression(10^6)), line=-0.6, lwd=0, las=1, cex=cex, cex.axis=cex)
mtext(side=1, line=0.6, "Number of reverted edits", cex=cex)
mtext(side=2, line=1.25, "Number of reverts", cex=cex)
lines(data$V1, exp(a + b * data.log$V1), col="#006699", lwd=2)
legend("topright", legend = c("Reverts", "Exponential model"), pch=c(1,-1), lty=c(-1,1), lwd=c(0,2), col=c("#000000", "#006699"), bty="n", cex=cex, seg.len=0.5)

dev.off()
