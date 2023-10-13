library(data.table);
setwd("C:\\MyFiles\\00 CURRENT\\03 PROJECTS\\Essex\\SimPaths\\02 PARAMETERISE\\STARTING DATA\\data\\");
data <- fread("population_UK_initial.csv");
data[, check:=sapply(idpartner, function(i) any(i %in% idperson)), by = idhh];
data[, checkm:=sapply(idmother, function(i) any(i %in% idperson)), by = idhh];
data[, checkf:=sapply(idfather, function(i) any(i %in% idperson)), by = idhh];
data;
fwrite(data, "population_UK_initial_check.csv");
