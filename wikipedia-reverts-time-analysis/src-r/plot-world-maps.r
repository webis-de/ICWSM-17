args.all <- commandArgs(trailingOnly=FALSE)
source(paste(dirname(sub("--file=", "", args.all[grep("--file=", args.all)])), "map-tools.r", sep="/"))

args <- commandArgs(trailingOnly=TRUE)
values.bycountry <- read.table(args[1], stringsAsFactors=FALSE)
values.bytimezone <- values.merge.microsoft(read.table(args[2], stringsAsFactors=FALSE))
reverted.threshold <- as.numeric(args[3])
output.file.base <- args[4]
output.country <- args[5]


plot.maps <- function(map.countries, map.countries.us, map.countries.europe, map.timezones.us, map.states.us, col.world, col.us, col.europe, lwd = pdf.lwd) {
  plot.map(map.countries, col.world, map.border.dark = TRUE, xlim = world.xlim, ylim = world.ylim, lwd = lwd)
  inlay.indian <- plot.inlay.fig.get(inlay.indian.user)
  inlay.pacific <- plot.inlay.fig.get(inlay.pacific.user)
  plot.inlay.prepare(fig = inlay.indian)
  plot.map(map.countries.europe, col.europe, map.border.dark = TRUE, xlim = europe.xlim, ylim = europe.ylim, lwd = lwd)
  plot.inlay.prepare(fig = inlay.pacific)
  plot.map(map.timezones.us, col.us, map.border.dark = map.countries.us, map.border.light = map.states.us, xlim = us.xlim, ylim = us.ylim, lwd = lwd)
}


map.timezones.us <- read.map.timezones()
map.countries <- read.map.countries()
map.states.us <- read.map.states()

map.countries.us <- filter.map.countries.us(map.countries)
map.countries.europe <- filter.map.countries.europe(map.countries)

col.world.ratio <- colors.ratio.pick(map.countries$ISO2, values.bycountry, reverted.threshold)
col.us.ratio <- colors.ratio.pick(map.timezones.us$TZID, values.bytimezone, reverted.threshold)
col.europe.ratio <- colors.ratio.pick(map.countries.europe$ISO2, values.bycountry, reverted.threshold)

col.world.absolute <- colors.absolute.pick(map.countries$ISO2, values.bycountry)
col.us.absolute <- colors.absolute.pick(map.timezones.us$TZID, values.bytimezone)
col.europe.absolute <- colors.absolute.pick(map.countries.europe$ISO2, values.bycountry)

pdf(paste(output.file.base, "-ratio-", output.country, ".pdf", sep=""), width=sum(world.layout.widths), world.layout.height, bg="transparent")
plot.ratio.prepare()
plot.maps(map.countries, map.countries.us, map.countries.europe, map.timezones.us, map.states.us, col.world.ratio, col.us.ratio, col.europe.ratio)
dev.off()

pdf(paste(output.file.base, "-absolute-", output.country, ".pdf", sep=""), width=sum(world.layout.widths), world.layout.height, bg="transparent")
plot.absolute.prepare()
plot.maps(map.countries, map.countries.us, map.countries.europe, map.timezones.us, map.states.us, col.world.absolute, col.us.absolute, col.europe.absolute)
dev.off()

