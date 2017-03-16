
get.date <- function(days, date.format="%Y-%m-%d") {
  return(format(as.POSIXct(days*24*60*60, origin="1970-01-01", tz="UTC"), format=date.format))
}

axis.time <- function(days, date.format="%Y-%m-%d") {
  first_days <- days[get.date(days, "%m-%d") == "01-01"]
  axis(1, at=first_days, labels=get.date(first_days, date.format=date.format))
}

