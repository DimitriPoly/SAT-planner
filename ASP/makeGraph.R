#pour que le script fonctionne, il faut l'exécuter dans le working directory du projet ASP
#pour cela, il faut ouvrir ce fichier depuis son emplacement dans l'explorateur de fichier
#(cela set automatiquement le working directory à l'emplacement du script)
library(readr)
DATA <- read_delim("DATA.csv", delim = ";", 
                   escape_double = FALSE, col_names = FALSE, 
                   trim_ws = TRUE)
DATA = data.frame(DATA[,-7])
View(DATA)
as.numeric(DATA[1,])


algos = c("SAT", "HSP")
problemNames = c("p001.pddl", "p002.pddl", "p003.pddl", "p004.pddl", "p005.pddl", "p006.pddl")
metrics = c("Total runtime (ms)", "Makespan (plan length)")
benchmarks = c("Blocksworld", "Depots", "Gripper", "Logistics")

par(mfrow=c(2, 4)) #permet de diviser la fenetre pour afficher tous les graphiques
index = 1
for (bench in benchmarks) { #on parcourt les benchmarks
  for (metric in metrics) { #on parcourt les metrics
    #le vecteur qui construit la matrice prend d'abord les valeurs pour SAT, puis pour HSP
    dataHSP = as.numeric(DATA[index,]); index <- index + 1;
    dataASP = as.numeric(DATA[index,]); index <- index + 1;
    order = order(dataHSP, decreasing = F)
    dataHSP <- dataHSP[order]
    dataASP <- dataASP[order]
    dataColnames <- problemNames[order]
    #les vecteurs de données sont maintenant triés avec les valeurs de HSP croissantes, et
    #associées aux valeurs d'ASP correspondantes.
    data_table = matrix(c(dataHSP, dataASP), nrow = 2, byrow = T)
    row.names(data_table) <- algos
    colnames(data_table) <- dataColnames
    data_table
    
    barplot(height = data_table,
            xlab = "Problems",
            ylab = ifelse(metric == metrics[1], metrics[1], metrics[2]),
            beside = TRUE,
            col = c("green", "blue"),
            las=2)
  }
  mtext(paste("Benchmark:", bench), side = 3, line = 1, at = -0.5)
}

