args <- commandArgs(trailingOnly=TRUE)
input <- args[1]
output <- args[2]


data <- read.table(input)

pdf(output, width=7, height=5)

plot(ecdf(pmax(data[,1],0)/60), xlim=c(0,5), ylim=c(0,1), yaxp=c(0,1,10), main="Time within Double Submissions", xlab="Maximum time within (minutes)", ylab="Percentage of double submissions")
abline(h=(0:10)/10, v=0:5, col="gray", lty=3)

dev.off()
