args <- commandArgs(trailingOnly=TRUE)
input <- args[1]
wiki <- args[2]
output <- args[3]
cex <- 0.75


data <- read.table(input)
total <- rev(cumsum(rev(data$V2)))/1000000
vandalism <- rev(cumsum(rev(data$V3)))/1000000
maxx <- ceiling(max(total)) + (20 - ceiling(max(total)) %% 20)

pdf(output, width=2.5, height=1.5)

par(mar = c(1.5,2.0,0.25,0))
barplot(total, xpd=FALSE, col="white", ylim=c(0,maxx), width=1, space=0, yaxt="n")
par(new=TRUE)
barplot(vandalism, xpd=FALSE, col="lightgrey", ylim=c(0,maxx), width=1, space=0, yaxt="n")

axis(1, at=(0:10)+0.5, labels=FALSE, tck=-0.03)
axis(1, at=(0:5)*2+0.5, labels=(0:5)*2, lwd=0, line=-1, cex.axis=cex)
axis(2, at=(0:(maxx/20))*20, labels=FALSE, line=-0.1, tck=-0.03)
axis(2, at=(0:(maxx/20))*20, labels=(0:(maxx/20))*20, lwd=0, line=-0.6, las=1, cex.axis=cex)
mtext(side=1, line=0.6, "Minimum number of GeoDBs", cex=cex)
mtext(side=2, line=1.25, "Edits (in millions)", cex=cex)
text((1:11)-0.5,total,sprintf("%.1f", rev(cumsum(rev(data$V2)))/1000000), srt=0, adj=c(0.5,-0.25), cex=0.6*cex, xpd=NA)
text((1:11)-0.5,vandalism,sprintf("%.1f", rev(cumsum(rev(data$V3)))/1000000), srt=0, adj=c(0.5,-0.25), cex=0.6*cex, xpd=NA)
#text((9:11)-0.5,rev(cumsum(rev(data$V3)))[9:11]/1000000-1,sprintf("%.0f", rev(cumsum(rev(data$V3)))[9:11]/1000000), srt=-90, adj=c(0.075,0.5), cex=cex)
#legend("topright", bty="n", legend=c("Total", "Vandalism"), fill=c("white", "darkgrey"), inset=c(0.02,0))

dev.off()
