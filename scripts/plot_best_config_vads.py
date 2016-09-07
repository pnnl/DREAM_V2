print ("""
=========================================================================
Python script to plot NRAP DREAM output

Programmed by K. Mansoor - Mar 2016
=========================================================================

""")

# cd /erdfilespace/userspace/mansoor1/work/co2/gen3_analysis/analysis_gen3_rev1/lanl_pkg_20150326/pnnl_dream_software
# python plot_dreamout01.py objective_summary_best.csv


import sys, re, os, csv
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

if (len(sys.argv) != 2):
	print('\nUsage : plot_best_config_vads.py <csv file>\n')
	exit()


csvfile=sys.argv[1]


if not re.search('.csv$',csvfile.lower()):
	print('\nError - input file must be a *.csv file\n')
	exit()


new_folder = os.path.join(os.path.dirname(csvfile) + '/best_config_vad_plots')
print(new_folder)
if not os.path.isdir(new_folder): os.mkdir(new_folder)
else: print('already exists')


filepre=re.sub('.csv$','',csvfile)
fignamepdf='%s.pdf'%filepre

#get the data
names = []
ttds = set()
list_of_counts = []
with open(csvfile) as f:
        reader = csv.reader(f)
        for row in reader:
                names.append(row[0])
                count = {}
                for entry in row[1:-1]:
                        value = float(entry)
                        ttds.add(value)
                        if(value in count):
                                count[value] += 1
                        else:
                                count[value] = 1
                list_of_counts.append(count)
                

ttds = sorted(ttds)
to_plot_list = []
for count in list_of_counts:
        counts = []
        for ttd in ttds:
                if ttd in count: counts.append(count[ttd])
                else: counts.append(0)
        to_plot_list.append(counts)

#plot the data

i = 0
for x in to_plot_list:
        N = len(ttds)

        ind = np.arange(N)  # the x locations for the groups
        width = 0.35       # the width of the bars

        fig, ax = plt.subplots()
        rects1 = ax.bar(ind, x, width, color='r')

        # add some text for labels, title and axes ticks
        ax.set_ylabel('Number of Scenarios')
        ax.set_title('Number of Scenarios Detected by VAD')
        ax.set_xticks(ind + width)
        ax.set_xticklabels(ttds)

        #ax.legend(('Hmm'), ('Men'))


        def autolabel(rects):
            # attach some text labels
            for rect in rects:
                height = rect.get_height()
                ax.text(rect.get_x() + rect.get_width()/2., 0.90*height,
                        '%d' % int(height),
                        ha='center', va='bottom')

        autolabel(rects1)

        plt.savefig(new_folder + '/' + names[i] + '.pdf')
        plt.close()
        plt.close()
        i += 1

t1=float(time.clock())
print('\n  Done!! [%3ds] open %s\n\n\n' % (t1-t0,fignamepdf))

