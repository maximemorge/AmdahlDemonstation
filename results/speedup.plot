## Copyright (C) Dialectics 2016
## GNUPLOT script to illustrate Amdhal's law i.e. speedup/workers
set terminal png
set datafile separator ","
set output 'speedup.png'
set title "Speedup with 4 physical CPU cores which appears as 8 logical CPUs\n with hyper-threading"
set yrange [1:10]
set xrange [1:50]
set xlabel  "Number of workers"
set ylabel "Speedup"
# --- set blue line 
set style line 1 lc rgb '#0060ad' lt 1 lw 2 pt 7 ps .25
# --- set red red
set style line 2 lc rgb '#dd181f' lt 1 lw 2 pt 5 ps 1.5   
# --- plot the experimental data and the theorerical limit
plot 'speedup.csv' with linespoints ls 1 title 'Parallel execution of independent computations', \
     8 title 'Theoretical limit for 8 logical CPUs' with lines linestyle 2