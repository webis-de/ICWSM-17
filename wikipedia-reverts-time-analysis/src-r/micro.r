
table <- read.table("tmpde.txt")
table.season <- table[,c(2,3,5,6,7,8)]
table.weekday <- table[,c(2,4,5,6,7,8)]
indices <- table[,2] >= 12784 # 2005
indices <- table[,2] >= 13149 # 2006
indices <- table[,2] >= 13514 # 2007

l <- length(levels(table.season[,2]))
m.season <- matrix(0, nrow=24*l, ncol=3)
m.season[,1] <- c(sapply(1:l, function(x) rep(x, 24)))
m.season[,2] <- c(rep(0:23,l))

for (i in 1:dim(m.season)[1]) {
  season <- levels(table.season[,2])[m.season[i,1]]
  hour <- m.season[i,2]
  a <- table.season[indices & table.season[,2] == season & table.season[,3] == hour,4:5]
  r <- a[,2]/a[,1]
  r <- r[!is.na(r)]
  m.season[i,3] <- ave(r)[1]
}

plot(m.season[,2], m.season[,3], t="n", ylim=c(0,0.5))
for (s in 1:l) {
  indices <- m.season[,1] == s
  lines(m.season[indices,2], m.season[indices,3])
}


l <- length(levels(table.weekday[,2]))
m.weekday <- matrix(0, nrow=24*l, ncol=3)
m.weekday[,1] <- c(sapply(1:l, function(x) rep(x, 24)))
m.weekday[,2] <- c(rep(0:23,l))

for (i in 1:dim(m.weekday)[1]) {
  weekday <- levels(table.weekday[,2])[m.weekday[i,1]]
  hour <- m.weekday[i,2]
  a <- table.weekday[indices & table.weekday[,2] == weekday & table.weekday[,3] == hour,4:5]
  r <- a[,2]/a[,1]
  r <- r[!is.na(r)]
  m.weekday[i,3] <- ave(r)[1]
}

plot(m.weekday[,2], m.weekday[,3], t="n", ylim=c(0,0.5))
for (s in 1:l) {
  indices <- m.weekday[,1] == s
  lines(m.weekday[indices,2], m.weekday[indices,3])
}

