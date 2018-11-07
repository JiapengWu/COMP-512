import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import sys

fname2=""
fname=sys.argv[1]
if len(sys.argv) >2:
    fname2 = sys.argv[2]
    df2=pd.read_csv(fname2,sep=",")
    means2=df2['mean']
    stds2=df2['std']


df=pd.read_csv(fname, sep=',')
means = df['mean']
stds = df['std']
xs = df.iloc[:,1]

plt.errorbar(xs, means, yerr=stds, fmt='go--', linewidth=1, markersize=3, label=(" ").join(fname.split("_")[4:6]))
plt.title((" ").join(fname.split("_")[2:-2]))
if fname2!="":
    plt.errorbar(xs, means2, yerr=stds2, fmt='bo--', linewidth=1, markersize=3,label=(" ").join(fname2.split("_")[4:6]))
    plt.gca().legend(loc='upper right')
    plt.title("Single client")
plt.ylabel("Response time")
xname= "#clients" if fname.split("_")[3] == "load"  else "load(#transaction/sec)"
plt.xlabel(xname)
plt.show()
