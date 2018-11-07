# usage: 
# - plot difference in response time:
# python plot.py <_stats.csv measured at client> <_stats.csv measured at mw> --diff
# - plot a single plot of response time:
# python plot.py <_stats.csv>
# - plot the single client graph for 1 RM and 3 RM:
# python plot.py <1_rm_stats.csv> <3_rm_stats.csv>

import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import sys

fname2=""
diff=False

fname=sys.argv[1]
label1 = (" ").join(fname.split("_")[5:7])
if len(sys.argv) >2:
    fname2 = sys.argv[2]
    df2=pd.read_csv(fname2,sep=",")
    means2=df2['mean']
    stds2=df2['std']
    label2 = (" ").join(fname2.split("_")[5:7])


df=pd.read_csv(fname, sep=',')
means = df['mean']
stds = df['std']
xs = df.iloc[:,1]

if len(sys.argv)>3:
    diff =True
    label1 = "Client RT"
    label2 = "Middleware RT"
 
plt.errorbar(xs, means, yerr=stds, fmt='go--', linewidth=1, markersize=3, label=label1)
plt.title((" ").join(fname.split("_")[3:-2]))
if fname2!="":
    plt.errorbar(xs, means2, yerr=stds2, fmt='bo--', linewidth=1, markersize=3,label=label2)
    plt.gca().legend(loc='upper right')
    if not diff:
        plt.title("Single client")
plt.ylabel("Response time")
xname= "#clients" if fname.split("_")[3] == "load"  else "load(#transaction/sec)"
plt.xlabel(xname)
plt.show()
