print ("""
=========================================================================
Python script to plot DREAM optimial sensor configurations on 3-panel
plume map

Programmed by K. Mansoor - May 2016
=========================================================================

""")

# cd /erdfilespace/userspace/mansoor1/work/co2/gen3_analysis/analysis_gen3_rev1/lanl_pkg_20150326/pnnl_dream_software
# python plot_dream_3panel.v2.py solution_space.tabb best_configurations.csv 


import os, re, sys, glob, scipy
import matplotlib
matplotlib.use('Agg')
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import matplotlib as mpl
#from matplotlib.mlab import griddata
#from matplotlib import colors
#import scipy.ndimage
#from scipy import *
#from scipy.interpolate import Rbf
#from matplotlib import cm
import time
#from matplotlib.colors import LogNorm
import pandas as pd

t0=float(time.clock())

figfontsize=14

params = {'axes.labelsize': figfontsize,
          'legend.fontsize': figfontsize-2,
          'xtick.labelsize': figfontsize-2,
          'ytick.labelsize': figfontsize-2}
mpl.rcParams.update(params)
#new_map = matplotlib.colors.LinearSegmentedColormap.from_list('new_map', colors, N=256)
#cmap = colors.ListedColormap(['white', 'blue', 'red'])


labelsize=figfontsize

mpl.rcParams['contour.negative_linestyle'] = 'solid'


if (len(sys.argv) != 3):
	print '\nUsage : plot_dream_3panel.v2.py <xyz dat file>  <sensor best config file>\n'
	exit()

fname=sys.argv[1]
bcfile=sys.argv[2]

threshold=0.5 

new_folder = os.path.join(os.path.dirname(fname) + '/best_config_visualizations')
if not os.path.isdir(new_folder): os.mkdir(new_folder)

if not re.search('.tab$',fname.lower()):
	print '\nError - input file must be a *.tab file\n'
	exit()


filepre=new_folder + "/" + re.sub('.tab$','',os.path.basename(fname))

fignamepdf='%s.pdf'%(filepre)
fignamepng='%s.png'%(filepre)


df1=pd.read_csv(fname, sep=' ', low_memory=False)
cols=df1.columns.tolist()
fields=cols[3:]

#print fields
#print df1
#exit()

#vals=df1[field].values
#vals=np.abs(vals)
#if re.search('ph',fname.lower()): 
#	vals[vals <=  threshold]=1
#	vals[vals > threshold]=0
#else:
#	vals[vals <  threshold]=0
#	vals[vals >= threshold]=1
#df1[field]=vals



#df1.to_csv('df1.dat',sep='\t', float_format='%g',header=True,index=False)


xvals=np.sort(np.unique(df1.x))
yvals=np.sort(np.unique(df1.y))
zvals=np.sort(np.unique(df1.z))
zvals=zvals[::-1]
zfact=1

xmax=xvals[-1]+ (xvals[-1]-xvals[-2])/2
ymax=yvals[-1]+ (yvals[-1]-yvals[-2])/2
zmax=zvals[-1]+ (zvals[-1]-zvals[-2])/2

if xmax>5000: 
	figwidth,figheight=14,8
	height=0.40
	height2=0.35
	bottom=0.10
	bottom2=0.55
	width=0.50
	width2=0.27
	textbottom=0.78
	bboxleft=1.11
else: 
	figwidth,figheight=10,14
	height=0.20
	height2=0.65
	bottom=0.05
	bottom2=0.77
	width=0.45
	width2=0.32
	textbottom=0.88
	bboxleft=1.22

mpl.style.use('ggplot')



df3 = pd.DataFrame(columns=['run','cost','vol_aq_degraded','sensor_type','x','y','z'])

f = open(bcfile,'r');bcdat=f.readlines();f.close()
hdata=re.split('[,]',bcdat[0].strip())
sensind = hdata.index([i for i in hdata if re.search("sensor types", i.lower())][0])
costind = hdata.index([i for i in hdata if re.search("cost", i.lower())][0])
voldind = hdata.index([i for i in hdata if re.search("volume of aquifer degraded", i.lower())][0])
rc=0
for i,line in enumerate(bcdat[1:]):
	ldat=re.split('[,]',line.strip())
	for ldatv in ldat[sensind:]:
		sname,sx,sy,sz=re.split('[ ]+',re.sub('[\(\)]','',ldatv)   )
		df3.loc[rc]=[i+1, ldat[costind], ldat[voldind], sname,sx,sy,sz]
		rc+=1
df3=df3.convert_objects(convert_numeric=True)
df3=df3.sort(['run','sensor_type']).reset_index(drop=True)

##CATHERINE!!! Things between these matching comments should be easy style adjustments, with (hopefully) clear descriptions.

##All colors are are hex colors, use a tool like www.w3schools.com/colors/colors_picker.asp to find out what value to use
axislabelcolor = "#0000ff" #Color for the labels on the x and y axes of each plot. Original "#0000ff"
plotlinecolor = "#999999" #Color of the hash marks in the plots. Original "#999999"
legendlabelcolor = "#666666" #Color of the labels for the legend (File:, Configuration:, etc). Original "#666666"
legendvaluecolor = "#0000cc" #Color of the values for the legend (solution_space.tab, etc). Original "#0000cc"
sensorbordercolor = "#ffffff" #Border of color around the sensor indicator. Original "ffffff"

##These colormaps are defined by Matplotlib, see http://matplotlib.org/examples/color/colormaps_reference.html
cloudfillcolormap = plt.cm.Blues #Color of the indicator for the cloud location. Original plt.cm.Blues

#Edges in the plots!
edgewidth = 0.2 #width of edges! Original 0.2

##These control the sensor look
sensormarker = 'o' #Style for the sensor, see http://matplotlib.org/api/markers_api.html#module-matplotlib.markers for valid options. Original 'o'
sensorsize = 12 #Size of the sensor marker. Original 12
sensorborderwidth = 2.2 #Width of the border on the sensor marker. Original 2.2

##Assuming that we want one font for everything. If you ask nicely, Luke will make italic options, etc.
universalfont = {'fontname':'Bitstream Vera Sans'} #Font! -- everything except for the legend for some reason. Original: Bitstream Vera Sans (This will also be the default if it can't find the font you want)
legendfontsize = 18 #Font size for the legend, the rest is set based on labelfontsize at the beginning of the file. Original 18

##CATHERINE!!! Things between these matching comments should be easy style adjustments, with (hopefully) clear descriptions.

for run, group in df3.groupby(['run']):
	
	for field in fields:

		group2=group[group.sensor_type==field].reset_index(drop=True)
		
		fignamepdf='%s.configuration%03d.%s.pdf'%(filepre, run, field)
		fignamepng='%s.configuration%03d.%s.png'%(filepre, run, field)
	
		print '\n  Creating figure:  %s'%fignamepng	
		cost=group.cost.values[0]
		vad=group.vol_aq_degraded.values[0]
	

		fig=plt.figure(figsize=(figwidth,figheight))

		print "  1. Plotting XY plane: "
		X, Y = np.meshgrid(xvals, yvals)
		dfxy=df1.groupby(['y','x']).max().reset_index()
		Z=np.abs(np.flipud(np.reshape(dfxy[field],(X.shape[0],X.shape[1]))))*zfact
		##plt.axes([0.05, 0.75, 0.35, 0.25]).set_aspect('equal')  #left, bottom, width, height
		ax1=plt.axes([0.09, bottom2, width, height]) #left, bottom, width, height
		plt.pcolor(X,Y,Z, alpha=0.55, cmap=cloudfillcolormap, linewidth=edgewidth, edgecolors=plotlinecolor,vmin=0.9, vmax=1.1)
		for i, row in group2.iterrows():plt.plot(row.x, row.y, sensormarker, ms=sensorsize, mew=sensorborderwidth, mec=sensorbordercolor, label='%02d %s (%1.1f,%1.1f,%1.1f)'%(i+1, row.sensor_type,row.x, row.y,row.z), alpha=0.75)
			#plt.text(row.x, row.y-300, row.sensor_type,fontsize=10,color='#000066', ha='center' )
	
		plt.xlim(np.min(X), np.max(X))
		plt.ylim(np.min(Y), np.max(Y))

		plt.xlabel('X (m)', size=labelsize, color=axislabelcolor, **universalfont)
		plt.ylabel('Y (m)', size=labelsize, color=axislabelcolor, **universalfont)

		#l=ax1.legend(loc='center left', bbox_to_anchor(1,0.815), numpoints=1, ncol=1, fancybox=False, shadow=False)
		l=ax1.legend(loc='lower left', bbox_to_anchor=(bboxleft, 0.00), ncol=1, numpoints=1)

		print "  1. Plotting XZ plane: "
		X, Y = np.meshgrid(xvals, zvals)
		dfxz=df1.groupby(['z','x']).max().reset_index()
		Z=np.abs(np.flipud(np.reshape(dfxz[field],(X.shape[0],X.shape[1]))))*zfact
		#fig.subplots_adjust(left=0.16, bottom=0.05, top=0.92, right=0.81)
		#plt.axes([0.05, 0.75, 0.35, 0.25]).set_aspect('equal')  #left, bottom, width, height
		ax2=plt.axes([0.09, bottom, width, height2]) #left, bottom, width, height
		plt.pcolor(X,Y,Z, alpha=0.55, cmap=cloudfillcolormap, linewidth=edgewidth, edgecolors=plotlinecolor,vmin=0.9, vmax=1.1)
		#plt.contour(X,Y,Z,([0,1]),linewidth=1, alpha=1.0)

	
		#for i, row in group.iterrows(): plt.plot([row.x,row.x], [np.max(Y), np.max(Y)- row.z], '-', lw=2, mew=0.2, alpha=0.5)
		#for i, row in group2.iterrows(): plt.plot(row.x, np.max(Y)-row.z, sensormarker, ms=sensorsize, mew=sensorborderwidth, mec=sensorbordercolor, label='%02d %s (%1.1f,%1.1f,%1.1f)'%(i+1, row.sensor_type,row.x, row.y,row.x), alpha=0.75)
		for i, row in group2.iterrows(): plt.plot(row.x, row.z, sensormarker, ms=sensorsize, mew=sensorborderwidth, mec=sensorbordercolor, label='%02d %s (%1.1f,%1.1f,%1.1f)'%(i+1, row.sensor_type,row.x, row.y,row.z), alpha=0.75)


		plt.xlim(np.min(X), np.max(X))
		plt.ylim(np.min(Y), np.max(Y))
		plt.xlabel('X (m)', size=labelsize, color=axislabelcolor, **universalfont)
		plt.ylabel('Z (m)', size=labelsize, color=axislabelcolor, **universalfont)
		fig.savefig(fignamepng)


		print "  1. Plotting YZ plane: "
		X, Y = np.meshgrid(yvals, zvals)
		dfyz=df1.groupby(['z','y']).max().reset_index()
		Z=np.abs(np.flipud(np.reshape(dfyz[field],(X.shape[0],X.shape[1]))))*zfact
		#fig.subplots_adjust(left=0.16, bottom=0.05, top=0.92, right=0.81)
		#plt.axes([0.05, 0.75, 0.35, 0.25]).set_aspect('equal')  #left, bottom, width, height
		ax3=plt.axes([0.65, bottom, width2, height2]) #left, bottom, width, height
		plt.pcolor(X,Y,Z, alpha=0.55, cmap=cloudfillcolormap, linewidth=edgewidth, edgecolors=plotlinecolor,vmin=0.9, vmax=1.1)
		#for i, row in group.iterrows(): plt.plot([row.y,row.y], [np.max(Y), np.max(Y)- row.z], '-', lw=2, mew=0.2, alpha=0.5)
		for i, row in group2.iterrows(): plt.plot(row.y, row.z, sensormarker, ms=sensorsize, mew=sensorborderwidth, mec=sensorbordercolor, label='%02d %s (%1.1f,%1.1f,%1.1f)'%(i+1, row.sensor_type,row.x, row.y,row.z), alpha=0.75)

		plt.xlim(np.min(X), np.max(X))
		plt.ylim(np.min(Y), np.max(Y))
		plt.xlabel('Y (m)', size=labelsize, color=axislabelcolor, **universalfont)
		plt.ylabel('Z (m)', size=labelsize, color=axislabelcolor, **universalfont)
	
	
		fig.text(0.73,textbottom,'File:\nConfiguration:\nField:\nVol Aq. Deg.:\nCost:',horizontalalignment='right', color=legendlabelcolor, fontsize=legendfontsize, **universalfont)
		fig.text(0.74,textbottom,'%s\n%03d\n%s\n%1.1f\n$%6.2f'%(fname,run,field,vad,cost),horizontalalignment='left', color=legendvaluecolor, fontsize=legendfontsize, **universalfont)


		fig.savefig(fignamepng)
		#plt.show()
		plt.close()
	
		#exit()
	

t1=float(time.clock())
print '\n\n  Done!![%4ds]   open %s.configuration???.*.png\n\n'%(t1-t0,filepre)



