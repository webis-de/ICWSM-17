args <- commandArgs(trailingOnly=TRUE)

table <- read.table(args[1], stringsAsFactors=FALSE)
column.day <- as.numeric(args[2])
day.first <- as.numeric(args[3])
column.diff <- as.numeric(args[4])
diff.one.key <- args[5]
diff.two.key <- args[6]
column.reverted <- as.numeric(args[7])

get.values <- function(key) {
  table.sub <- table[grepl(key, table[,column.diff]) & table[,column.day] >= day.first,]
  values <- table.sub[,column.reverted+1] / table.sub[,column.reverted]
  return(values[!is.na(values)])
}

# Welch Two Sample t-test
welch_t_test <- function(x, y) {
  pvalue <- t.test(x, y, var.equal=FALSE, paired=FALSE, alternative="less")$p.value
  stars <- ""
  if (pvalue <= 0.05) {
    stars <- "*"
    if (pvalue <= 0.01) {
      stars <- "**"
      if (pvalue <= 0.001) {
        stars <- "***"
      }
    }
  }
  return(stars)
}

cohens_d <- function(x, y) {
    lx <- length(x)- 1
    ly <- length(y)- 1
    md  <- abs(mean(x) - mean(y))        ## mean difference (numerator)
    csd <- lx * var(x) + ly * var(y)
    csd <- csd/(lx + ly)
    csd <- sqrt(csd)                     ## common sd computation

    cd  <- md/csd                        ## cohen's d

# Effect Size d
# Small    0.20
# Medium   0.50
# Large    0.80

    return(cd)
}

calculate.effect <- function(x, y) {
  d <- cohens_d(x, y)
  stars <- welch_t_test(x, y)
  return(sprintf("%.2f%s", d, stars))
}

diff.one.values <- get.values(diff.one.key)
diff.two.values <- get.values(diff.two.key)

print(calculate.effect(diff.one.values, diff.two.values))


