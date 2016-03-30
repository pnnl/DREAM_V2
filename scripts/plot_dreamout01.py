print ("""
=========================================================================
Python script to plot NRAP DREAM output

Programmed by K. Mansoor - Mar 2016
=========================================================================

""")

# cd /erdfilespace/userspace/mansoor1/work/co2/gen3_analysis/analysis_gen3_rev1/lanl_pkg_20150326/pnnl_dream_software
# python plot_dreamout01.py objective_summary_best.csv


import sys, re, os
import numpy as np
import time
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib as mpl
import matplotlib.mlab as mlab
import matplotlib.pyplot as plt
#from pandas.tools.plotting import table


t0=float(time.clock())

figfontsize=18

params = {'axes.labelsize': figfontsize+4,
          'font.size': figfontsize,
          'legend.fontsize': figfontsize-2,
          'xtick.labelsize': figfontsize,
          'ytick.labelsize': figfontsize}
mpl.rcParams.update(params)


if (len(sys.argv) != 2):
	print('\nUsage : ntab_boxplots_mid_y_slice.py <ntab file>\n')
	exit()


csvfile=sys.argv[1]


if not re.search('.csv$',csvfile.lower()):
	print('\nError - input file must be a *.ntab file\n')
	exit()

filepre=re.sub('.csv$','',csvfile)
fignamepdf='%s.pdf'%filepre

print('  Reading file: %s\n'%csvfile)
df1=pd.read_csv(csvfile, sep=',', index_col=[0], low_memory=False)
df1=df1[df1.index >=0]
df1 = df1.rename(columns=lambda x: re.sub('^ ','',x))


hdect=[i for i in df1.columns if re.search('detected', i.lower())]
httd=[i for i in df1.columns if re.search('ttd', i.lower())]
dfdect=df1[hdect]
dfttd=df1[httd]

dfdect = dfdect.rename(columns=lambda x: re.sub(' .*','',x))
dfttd = dfttd.rename(columns=lambda x: re.sub(' .*','',x))

mpl.style.use('ggplot')
figwidth=14
figheight=10
fig, (ax1,ax2) = plt.subplots(2,1, sharex=True, sharey=False, figsize=(figwidth,figheight))
plt.subplots_adjust(left=0.13, bottom=0.10, top=0.89, right=0.85)


dfdect.plot(marker='s', ms=0, ax=ax1, linewidth=3, mew=0.5,colormap='hsv', alpha=0.55)
ax1.set_ylabel('Scenarios with\nLeakage Detected (%)\n', fontsize=figfontsize)

dfttd.plot(marker='s', ms=0, ax=ax2, linewidth=3, mew=0.5,colormap='hsv', alpha=0.55)
ax2.set_ylabel('E(TFD) (years)\n', fontsize=figfontsize)
ax2.set_xlabel('\nIterations', fontsize=figfontsize)

ax1.legend(loc='center left', bbox_to_anchor=(1, 0.5))
ax2.legend(loc='center left', bbox_to_anchor=(1, 0.5))

plt.savefig(fignamepdf)
plt.close()
plt.close()
t1=float(time.clock())
print('\n  Done!! [%3ds] open %s\n\n\n' % (t1-t0,fignamepdf))

