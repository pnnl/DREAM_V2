print ("""
==========================plot_dream_3panel.py==========================
Plots DREAM optimial sensor configurations on 3-panel plume map

========================================================================
""")
#Programmed by K. Mansoor - May 2016
#Updated to Python3 & Added ERT by J. Whiting - February 2018
#Updated by J. Whiting - May 2019

# python plot_dream_3panel.py ensemble_solutionSpace.txt best_configurations.csv 

import os, re, sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib as mpl
import time
import pandas as pd

t0=float(time.clock())

# Make sure that we have the correct number of arguments, otherwise abort
if (len(sys.argv) != 3):
    print ('Usage : plot_dream_3panel.py <xyz dat file>  <sensor best config file>\n')
    exit()

# Arguments
fname=sys.argv[1]
bcfile=sys.argv[2]
#fname='C:\\Users\\whit162\\OneDrive - PNNL\\Documents\\Projects\\DreamProject\\Results_20191205155836\\BCO_new_solutionSpace.txt'
#bcfile='C:\\Users\\whit162\\OneDrive - PNNL\\Documents\\Projects\\DreamProject\\Results_20191205155836\\best_configurations.csv'
if not re.search('.txt$',fname.lower()):
    print ('\nError - input file must be a *.txt file\n')
    exit()

# Determine the directory where we save results
new_folder = os.path.join(os.path.dirname(fname) + '/best_config_visualizations')
print(new_folder)
if not os.path.isdir(new_folder): os.mkdir(new_folder)
else: print('directory already exists')

filepre=new_folder + "/" + re.sub('.txt$','',os.path.basename(fname))
fignamepng='%s.png'%(filepre)
ensemble = re.sub('_solutionSpace.txt','',os.path.basename(fname))

# Set values to control plot displays
figfontsize=14
labelsize=figfontsize
params = {'axes.labelsize': figfontsize,
          'legend.fontsize': figfontsize-2,
          'xtick.labelsize': figfontsize-2,
          'ytick.labelsize': figfontsize-2}
mpl.rcParams.update(params)
mpl.rcParams['contour.negative_linestyle'] = 'solid'
mpl.style.use('ggplot')
threshold=0.5

# Read in the solution space to df1
df1=pd.read_csv(fname, sep=' ', low_memory=False)
cols=df1.columns.tolist()
fields=cols[3:]

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
    width2=0.32
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

# Read in the best configurations to df3
df3 = pd.DataFrame(columns=['config','cost','vol_aq_degraded','sensor_type','x','y','z','x2','y2'])

f = open(bcfile,'r');bcdat=f.readlines();f.close()
hdata=re.split('[,]',bcdat[0].strip())
sensind = hdata.index([i for i in hdata if re.search("sensor types", i.lower())][0])
costind = hdata.index([i for i in hdata if re.search("cost", i.lower())][0])
voldind = hdata.index([i for i in hdata if re.search("volume of aquifer degraded", i.lower())][0])
rc=0
for i,line in enumerate(bcdat[1:]):
    ldat=re.split('[,]',line.strip())
    for ldatv in ldat[sensind:]:
        slist = re.split('[ ]+',re.sub('[\(\)]','',ldatv))
        if ldatv.startswith("ERT"):
            sname,sx,sy,sx2,sy2 = slist
            df3.loc[rc]=[i+1, ldat[costind], ldat[voldind], sname,sx,sy,0,sx2,sy2]
        else:
            sx,sy,sz=slist[-3:]
            sname=ldatv.split(' (')[0]
            df3.loc[rc]=[i+1, ldat[costind], ldat[voldind], sname,sx,sy,sz,0,0]
        rc+=1
df3.apply(pd.to_numeric, errors='ignore')
df3=df3.sort_values(by=['config','sensor_type'])

# Determine the units and add to the axis labels (if available)
vad = df3.at[0, 'vol_aq_degraded']
xLabel = 'X'
yLabel = 'Y'
zLabel = 'Z'
if '³' in vad:
    unit=re.split('[ ]+',re.sub('³','',vad))[1]
    xLabel = 'X (%s)'%unit
    yLabel = 'Y (%s)'%unit
    zLabel = 'Z (%s)'%unit

'''
Things between these matching comments should be easy style adjustments, with (hopefully) clear descriptions.
'''
##All colors are are hex colors, use a tool like www.w3schools.com/colors/colors_picker.asp to find out what value to use
axislabelcolor = "#0000ff" #Color for the labels on the x and y axes of each plot. Original "#0000ff"
plotlinecolor = "#999999" #Color of the hash marks in the plots. Original "#999999"
legendlabelcolor = "#666666" #Color of the labels for the legend (File:, Configuration:, etc). Original "#666666"
legendvaluecolor = "#0000cc" #Color of the values for the legend (solutionSpace.txt, etc). Original "#0000cc"
sensorbordercolor = "#ffffff" #Border of color around the sensor indicator. Original "ffffff"

##These colormaps are defined by Matplotlib, see http://matplotlib.org/examples/color/colormaps_reference.html
cloudfillcolormap = [plt.cm.Reds, plt.cm.Blues, plt.cm.Greens, plt.cm.Oranges, plt.cm.Purples, plt.cm.Greys] #Color of the indicator for the cloud location. Original plt.cm.Blues

#Edges in the plots!
edgewidth = 0.2 #width of edges! Original 0.2

##These control the sensor look
sensormarker = 'o' #Style for the sensor, see http://matplotlib.org/api/markers_api.html#module-matplotlib.markers for valid options. Original 'o'
sensorsize = 12 #Size of the sensor marker. Original 12
sensorborderwidth = 2.2 #Width of the border on the sensor marker. Original 2.2

##Assuming that we want one font for everything.
universalfont = {'fontname':'DejaVu Sans'} #Font! -- everything except for the legend for some reason. Original: DejaVu Sans (This will also be the default if it can't find the font you want)
legendfontsize = 18 #Font size for the legend, the rest is set based on labelfontsize at the beginning of the file. Original 18
'''
Things between these matching comments should be easy style adjustments, with (hopefully) clear descriptions.
'''
#### First, we create a map of the solution space per parameter ####
dfxy=df1.groupby(['y','x']).max().reset_index()
dfxz=df1.groupby(['z','x']).max().reset_index()
dfyz=df1.groupby(['z','y']).max().reset_index()

for field in fields:
    
    fignamepng='%s/SolutionSpace_%s.png'%(new_folder, field)
    print ('\nCreating figure: %s'%fignamepng)
    fig=plt.figure(figsize=(figwidth,figheight))
    
    print ("  1. Plotting XY plane")
    X,Y = np.meshgrid(xvals, yvals)
    Z = np.abs(np.flipud(dfxy[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax1=plt.axes([0.09, bottom2, width, height]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('X (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Y (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    print ("  2. Plotting XZ plane")
    X,Y = np.meshgrid(xvals, zvals)
    Z = np.abs(np.flipud(dfxz[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax2=plt.axes([0.09, bottom, width, height2]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('X (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Z (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    print ("  3. Plotting YZ plane")
    X,Y = np.meshgrid(yvals, zvals)
    Z = np.abs(np.flipud(dfyz[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax3=plt.axes([0.65, bottom, width2, height2]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('Y (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Z (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    # Adds the text to the upper right
    fig.text(0.73,textbottom,'Ensemble:\nField:',horizontalalignment='right', color=legendlabelcolor, fontsize=legendfontsize, **universalfont)
    fig.text(0.74,textbottom,'%s\n%s'%(ensemble,field),horizontalalignment='left', color=legendvaluecolor, fontsize=legendfontsize, **universalfont)
    
    fig.savefig(fignamepng)
    plt.close()

#### Now we create a map for each configuration ####
# Limit XY to new zoomed bounds
xMaxZoom = np.where(xvals==float(np.max(df3['x'])))[0][0] + round(len(xvals)/10)
xMinZoom = np.where(xvals==float(np.min(df3['x'])))[0][0] - round(len(xvals)/10)
yMaxZoom = np.where(yvals==float(np.max(df3['y'])))[0][0] + round(len(yvals)/10)
yMinZoom = np.where(yvals==float(np.min(df3['y'])))[0][0] - round(len(yvals)/10)
dfxy = dfxy[(dfxy.x < xvals[xMaxZoom]) & (dfxy.x >= xvals[xMinZoom])]
dfxy = dfxy[(dfxy.y < yvals[yMaxZoom]) & (dfxy.y >= yvals[yMinZoom])]
dfxz = dfxz[(dfxz.x < xvals[xMaxZoom]) & (dfxz.x >= xvals[xMinZoom])]
dfyz = dfyz[(dfyz.y < yvals[yMaxZoom]) & (dfyz.y >= yvals[yMinZoom])]
xvals = xvals[xMinZoom:xMaxZoom]
yvals = yvals[yMinZoom:yMaxZoom]

for config, group in df3.groupby(['config']):
    # Start by resetting to 0s
    for field in fields:
        dfxy.loc[dfxy[field]==1, field] = 0
        dfxz.loc[dfxz[field]==1, field] = 0
        dfyz.loc[dfyz[field]==1, field] = 0
    
    #Fill with values where sensors exist
    for row in group.itertuples():
        sensor = row.sensor_type
        dfxy.loc[(dfxy['x']==float(row.x)) & (dfxy['y']==float(row.y)), sensor] = 1
        dfxz.loc[(dfxz['x']==float(row.x)) & (dfxz['z']==float(row.z)), sensor] = 1
        dfyz.loc[(dfyz['y']==float(row.y)) & (dfyz['z']==float(row.z)), sensor] = 1
    
    fignamepng='%s/configuration%03d.png'%(new_folder, config)
    print ('\nCreating figure: %s'%fignamepng)
    cost=float(group.cost.values[0])
    vad=group.vol_aq_degraded.values[0]
    fig=plt.figure(figsize=(figwidth,figheight))
    
    print ("  1. Plotting XY plane")
    X,Y = np.meshgrid(xvals, yvals)
    Z = np.abs(np.flipud(dfxy[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax1=plt.axes([0.09, bottom2, width, height]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('X (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Y (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    print ("  2. Plotting XZ plane")
    X,Y = np.meshgrid(xvals, zvals)
    Z = np.abs(np.flipud(dfxz[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax2=plt.axes([0.09, bottom, width, height2]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('X (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Z (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    print ("  3. Plotting YZ plane")
    X,Y = np.meshgrid(yvals, zvals)
    Z = np.abs(np.flipud(dfyz[field].values.reshape((X.shape[0],X.shape[1]))))*zfact
    ax3=plt.axes([0.65, bottom, width2, height2]) #left, bottom, width, height
    plt.pcolor(X,Y,Z, alpha=1, cmap=cloudfillcolormap[fields.index(field)], linewidth=edgewidth, edgecolors=plotlinecolor)
    plt.xlim(np.min(X), np.max(X))
    plt.ylim(np.min(Y), np.max(Y))
    plt.xlabel('Y (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    plt.ylabel('Z (%s)'%unit, size=labelsize, color=axislabelcolor, **universalfont)
    
    colorMap = ['red', 'blue', 'green', 'orange', 'purple', 'grey']
    patchMap = []
    for field in fields:
        patchMap.append(mpatches.Patch(color=colorMap[fields.index(field)], label=field))
    l=ax1.legend(handles=patchMap, loc='lower left', bbox_to_anchor=(bboxleft, 0.00))
    
    # Adds the text to the upper right
    fig.text(0.73,textbottom,'Ensemble:\nConfiguration:\nVol Aq. Deg.:\nCost:',horizontalalignment='right', color=legendlabelcolor, fontsize=legendfontsize, **universalfont)
    fig.text(0.74,textbottom,'%s\n%03d (zoomed view)\n%s\n$%s'%(ensemble,config,vad,cost),horizontalalignment='left', color=legendvaluecolor, fontsize=legendfontsize, **universalfont)
    
    fig.savefig(fignamepng)
    plt.close()

t1=float(time.clock())
print ('\n\n  Done!![%4ds]   open %s/configuration*.*.png\n\n'%(t1-t0,new_folder))

