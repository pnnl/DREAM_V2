print ("""
===========================plot_dreamout01.py===========================
Python script to plot NRAP DREAM output

Programmed by K. Mansoor - Mar 2016
Updated by J. Whiting - Dec 2019
========================================================================
""")

# python plot_dreamout01.py objective_summary_best.csv


import re, sys
import matplotlib.pyplot as plt
import matplotlib as mpl
import time
import pandas as pd
import random
from random import randint


t0=float(time.process_time())

# Make sure that we have the correct number of arguments, otherwise abort
if (len(sys.argv) != 2):
	print('\nUsage : ntab_boxplots_mid_y_slice.py <ntab file>\n')
	exit()

# Arguments
csvfile=sys.argv[1]
#csvfile='C:\\Users\\whit162\\OneDrive - PNNL\\Documents\\Projects\\DreamProject\\Results_20191206122946\\objective_summary_best.csv'	

if not re.search('.csv$',csvfile.lower()):
	print('\nError - input file must be a *.ntab file\n')
	exit()

filepre=re.sub('.csv$','',csvfile)
fignamepdf='%s.pdf'%filepre 

# Set values to control plot displays
figfontsize=18
params = {'axes.labelsize': figfontsize+4,
          'font.size': figfontsize,
          'legend.fontsize': figfontsize-2,
          'xtick.labelsize': figfontsize,
          'ytick.labelsize': figfontsize}
mpl.rcParams.update(params)

# Read in the solution space to df1
print('  Reading file: %s\n'%csvfile)
df1=pd.read_csv(csvfile, sep=',', index_col=[0], low_memory=False)
df1=df1[df1.index >=0]
df1 = df1.rename(columns=lambda x: re.sub('^ ','',x))


hdect=[i for i in df1.columns if re.search('detected', i.lower())]
httd=[i for i in df1.columns if re.search('ttd', i.lower())]
dfdect=df1[hdect]
dfttd=df1[httd]

dfdect = dfdect.rename(columns=lambda x: re.sub(':.*','',x))
dfttd = dfttd.rename(columns=lambda x: re.sub(':.*','',x))
unit = dfttd.at[0, dfttd.columns[0]].split(' ')[-1]
dfttd = dfttd.replace({r"[a-zA-Z]":''}, regex=True)
dfdect = dfdect.replace({"%":''}, regex=True)
dfttd=dfttd.astype(float)
dfdect=dfdect.astype(float)

#Assign colors to each run
random.seed(1)
color = []
n = len(dfttd.columns)
for i in range(n):
    color.append('#%06X' % randint(0, 0xFFFFFF))
color_dict = dict(zip(dfttd.columns, color))

mpl.style.use('ggplot')
figwidth=14
figheight=10
fig, (ax1,ax2) = plt.subplots(2,1, sharex=True, sharey=False, figsize=(figwidth,figheight))
plt.subplots_adjust(left=0.13, bottom=0.10, top=0.89, right=0.85)

dfdect.plot(marker='s', ms=0, ax=ax1, linewidth=3, mew=0.5,color=[color_dict.get(x, '#333333') for x in dfdect.columns], alpha=0.85)
ax1.set_ylabel('Scenarios with\nLeakage Detected (%)\n', fontsize=figfontsize)

dfttd.plot(marker='s', ms=0, ax=ax2, linewidth=3, mew=0.5,color=[color_dict.get(x, '#333333') for x in dfttd.columns], alpha=0.85)
ax2.set_ylabel('Time to Detection (%s)\n'%(unit), fontsize=figfontsize)
ax2.set_xlabel('\nIterations', fontsize=figfontsize)

ax1.legend(loc='center left', bbox_to_anchor=(1, 0.5))
ax2.legend(loc='center left', bbox_to_anchor=(1, 0.5))

plt.savefig(fignamepdf)
plt.close()
t1=float(time.process_time())
print('\n  Done!! [%3ds] open %s\n\n\n' % (t1-t0,fignamepdf))

