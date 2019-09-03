"""
Copyright (c) 2019, Lawrence Livermore National Security, LLC.  All rights reserved.  LLNL-CODE-773881.

OFFICIAL USE ONLY – EXPORT CONTROLLED INFORMATION

This work was produced at the Lawrence Livermore National Laboratory (LLNL) under contract no. DE-AC52-07NA27344 (Contract 44) between the U.S. Department of Energy (DOE) and Lawrence Livermore National Security, LLC (LLNS) for the operation of LLNL.  See license for disclaimers, notice of U.S. Government Rights and license terms and conditions.

-------

[About]
 gravgrad computes gravity field and gravity gradient tensor resulting from a system of right rectangular prisms. The original Fortran IV(77) code was published by Montana et al. (1992):
 
 Montana, C. J., K. L. Mickus, and W. J. Peeples (1992), Program to calculate the gravitational field and gravity gradient tensor resulting from a system of right rectangular prisms, Comput Geosci-Uk, 18(5), 587-602.

Kevin Mickus, one of the original authors, shared an updated code still in Fortran 77 in November, 2017

 The original Fortran 77 program was ported to Python 3.6
 by Xianjin Yang, LLNL, 4/29/2019

[Global variables] - all distances are in meters
  nxpts,nypts - number of x and y observations points
           z0 - height of the observations (m), earth's surface = 0
    delrho(i) - density (contrast) of each prism (gm/cc)
  do_gradient - if do_gradient = 0 : calculate and save gz and gy
                                     gx is the transpose of gy
                if do_gradient = 1 : calculate & save gz,gy,gzx,gzy,gzz,gxx,gyx,gyy
           np - number of prisms
         iave - 0 for removal of mean gravity in the output file
        x0(i) - x coordinate of observation points
        y0(i) - y coordinate of observation points
      xc(1,j), xc(2,j) - the sides of the jth prism in the x-direction
      yc(1,j), yc(2,j) - the sides of the jth prism in the y-direction
      zc(1,j), zc(2,j) - the sides of the jth prism in the z-direction
      xc(2,j) is always larger than xc(1,j). Same for yc and zc
      
This programs requires Python 3.6 and newer version

"""

import sys, math
import numpy as np

usage = '''
  python gravgrad.py input_file [output_file]
    input_file: input file name. It contains density model and 
                observation grid
    output_file: optional output file name. It saves forward 
                modeling solution. If missing, it is set to 
                input_file + '.fwd'
  
  Examples:
    python gravgrad.py one_prism.in one_prism.in.fwd
    python gravgrad.py sim0001_50y.in [sim0001_50y.in.fwd]

'''

cTiny = 1.0e-29  # to avoid comparison with 0.0

#----------------
class GravGrad():
    def __init__(self):
        if len(sys.argv) < 2:
            print(usage)
            sys.exit(1)
        self.infile = str(sys.argv[1])
        if len(sys.argv) > 2:
            extension = '.in'
            ind = self.infile.index(extension)
            self.outfile = self.infile[:ind] + "_" + str(sys.argv[2]) + '.fwd'
        else:
            extension = '.in'
            ind = self.infile.index(extension)
            self.outfile = self.infile[:ind] + "_1" + '.fwd'

    #-----------------
    def input_density_model(self):
        print('''Read observation grid and density model''')
        with open(self.infile, 'r') as fin:
            lines = fin.readlines()
        tokens = lines[0].split()
        self.nxpts = int(tokens[0])
        self.nypts = int(tokens[1])
        tokens = lines[1].split()
        # The calculations in this code are in kilometers
        # but the distances in the input file are in meters
        self.z0 = 0.001*float(tokens[0])
        self.nprisms = int(tokens[1])
        self.iave = int(tokens[2])
        self.do_gradient = int(tokens[3])
        tokens = lines[2].split()
        self.x0 = 0.001*np.array([float(x) for x in tokens])
        tokens = lines[3].split()
        self.y0 = 0.001 * np.array([float(x) for x in tokens])

        # initialize arrays of prism bounds and density changes
        self.xc = np.zeros((2, self.nprisms))
        self.yc = np.zeros((2, self.nprisms))
        self.zc = np.zeros((2, self.nprisms))
        self.delrho = np.zeros(self.nprisms)

        # After 4 headers, the prism models are listed one prism per row
        data = 0.001*np.loadtxt(self.infile, skiprows=4, dtype=np.float32)
        if data.ndim == 1:  # only one rectangular prism
            data = data.reshape((1, data.size))
        self.xc[0,:] = np.copy(data[:,0])
        self.xc[1,:] = np.copy(data[:,1])
        self.yc[0,:] = np.copy(data[:,2])
        self.yc[1,:] = np.copy(data[:,3])
        self.zc[0,:] = np.copy(data[:,4])
        self.zc[1,:] = np.copy(data[:,5])
        # the density data was multiplied by 0.001 above
        # Now multiply it by 1000 to recover its original value
        self.delrho = 1000.0*np.copy(data[:,6])

    # ------------------------
    def compute_gravity(self):
        print('Compute gravity anomaly and gradients')
        g = -6.67408
        g1 = -66.73
        yvert = np.zeros(5)
        zvert = np.zeros(5)
        # gravity and  gradient anomalies
        self.gz  = np.zeros((self.nxpts, self.nypts))
        self.gy  = np.zeros((self.nxpts, self.nypts))
        self.gzx = np.zeros((self.nxpts, self.nypts))
        self.gzy = np.zeros((self.nxpts, self.nypts))
        self.gzz = np.zeros((self.nxpts, self.nypts))
        self.gyx = np.zeros((self.nxpts, self.nypts))
        self.gyy = np.zeros((self.nxpts, self.nypts))
        self.gxx = np.zeros((self.nxpts, self.nypts))

        #  loop over number of prisms
        for i70 in range(self.nprisms):
            x1 = self.xc[0, i70]
            x2 = self.xc[1, i70]
            yvert[0] = self.yc[0, i70]
            yvert[1] = yvert[0]
            yvert[2] = self.yc[1, i70]
            yvert[3] = yvert[2]
            zvert[0] = self.zc[0, i70]
            zvert[1] = self.zc[1, i70]
            zvert[2] = zvert[1]
            zvert[3] = zvert[0]
            yvert[4] = yvert[0]
            zvert[4] = zvert[0]
            #-------- start actual calculation ---
            for j40 in range(self.nypts):     #  y loc of sensors
                for i30 in range(self.nxpts): # x loc of sensors
                    xl1 = x1 - self.x0[i30]
                    xl2 = x2 - self.x0[i30]

                    temp = np.zeros(7)
                    for k20 in range(4):  # 4 sides of a prism
                        deltay = yvert[k20 + 1] - yvert[k20]
                        deltaz = zvert[k20 + 1] - zvert[k20]
                        hypot = math.sqrt(deltay * deltay + deltaz * deltaz)
                        sine = deltaz / hypot
                        cose = deltay / hypot
                        ndotz = deltay / hypot
                        ndoty = -deltaz / hypot

                        #  observation location [x0,y0,z0]
                        yk = yvert[k20] - self.y0[j40]
                        ykp1 = yvert[k20 + 1] - self.y0[j40]
                        zk = zvert[k20] - self.z0
                        zkp1 = zvert[k20 + 1] - self.z0

                        vo = cose * self.y0[j40] + sine * self.z0
                        vk = cose * yvert[k20] + sine * zvert[k20] - vo
                        vkp1 = cose * yvert[k20 + 1] + sine * zvert[k20 + 1] - vo

                        wo = -sine * self.y0[j40] + cose * self.z0
                        wk = -sine * yvert[k20] + cose * zvert[k20] - wo
                        wkp1 = wk

                        rkx1 = self.r(xl1, vk, wk)
                        rkx2 = self.r(xl2, vk, wk)
                        rkp1x1 = self.r(xl1, vkp1, wkp1)
                        rkp1x2 = self.r(xl2, vkp1, wkp1)

                        int1 = +(self.t1(xl2, vkp1, rkp1x2) - self.t1(xl2, vk, rkx2)) \
                               - (self.t1(xl1, vkp1, rkp1x1) - self.t1(xl1, vk, rkx1))

                        int2 = +(self.t2(xl2, vkp1, rkp1x2) - self.t2(xl1, vkp1, rkp1x1)) \
                               - (self.t2(xl2, vk, rkx2) - self.t2(xl1, vk, rkx1))

                        int3 = +(self.t3(xl2, vkp1, wkp1, rkp1x2) - self.t3(xl2, vk, wk, rkx2)) \
                               - (self.t3(xl1, vkp1, wkp1, rkp1x1) - self.t3(xl1, vk, wk, rkx1))

                        # contribution of edge to gravity field
                        temp[0] = temp[0] + (int1 + int2 + int3) * ndoty
                        temp[1] = temp[1] + (int1 + int2 + int3) * ndotz

                        #  contribution of edge to gradient of gravity field
                        if self.do_gradient == 1:
                            qx1 = self.q(xl1, rkx1, wk)
                            qx2 = self.q(xl2, rkx2, wk)
                            qpx1 = self.q(xl1, rkp1x1, wkp1)
                            qpx2 = self.q(xl2, rkp1x2, wkp1)

                            dx1 = +(self.t1x(vkp1, rkp1x2) - self.t1x(vkp1, rkp1x1))  \
                                  - (self.t1x(vk, rkx2) - self.t1x(vk, rkx1))

                            dx2 = +(self.t2x(xl2, vkp1, rkp1x2) - self.t2x(xl1, vkp1, rkp1x1)) \
                                  - (self.t2x(xl2, vk, rkx2) - self.t2x(xl1, vk, rkx1))

                            dx3 = +(self.t3x(qpx2, xl2, rkp1x2, vkp1, wk)
                                  -  self.t3x(qpx1, xl1, rkp1x1, vkp1, wk)) \
                                  - (self.t3x(qx2, xl2, rkx2, vk, wk) -
                                     self.t3x(qx1, xl1, rkx1, vk, wk))
                            #
                            dy1 = +(self.t1y(xl2, ykp1, vkp1, rkp1x2, cose)
                                  - self.t1y(xl2, yk, vk, rkx2, cose))       \
                                  - (self.t1y(xl1, ykp1, vkp1, rkp1x1, cose)
                                     - self.t1y(xl1, yk, vk, rkx1, cose))
                            #
                            dy2 = +(self.t2y(xl2, ykp1, vkp1, rkp1x2, cose)
                                  - self.t2y(xl1, ykp1, vkp1, rkp1x1, cose)) \
                                  - (self.t2y(xl2, yk, vk, rkx2, cose)
                                  - self.t2y(xl1, yk, vk, rkx1, cose))
                            #
                            dy3 = +(self.t3y(qpx2, xl2, ykp1, rkp1x2, vkp1, wkp1, cose, sine)
                                  - self.t3y(qpx1, xl1, ykp1, rkp1x1, vkp1, wkp1, cose, sine)) \
                                  - (self.t3y(qx2, xl2, yk, rkx2, vk, wk, cose, sine)
                                     - self.t3y(qx1, xl1, yk, rkx1, vk, wk, cose, sine))
                            #
                            dz1 = +(self.t1z(xl2, vkp1, zkp1, rkp1x2, sine)
                                  - self.t1z(xl2, vk, zk, rkx2, sine))       \
                                  - (self.t1z(xl1, vkp1, zkp1, rkp1x1, sine)
                                     - self.t1z(xl1, vk, zk, rkx1, sine))
                            #
                            dz2 = +(self.t2z(xl2, vkp1, zkp1, rkp1x2, sine)
                                  - self.t2z(xl1, vkp1, zkp1, rkp1x1, sine)) \
                                  - (self.t2z(xl2, vk, zk, rkx2, sine)
                                  - self.t2z(xl1, vk, zk, rkx1, sine))
                            #
                            dz3 = +(self.t3z(qpx2, xl2, zkp1, rkp1x2, vkp1, wkp1, cose, sine)
                                  - self.t3z(qpx1, xl1, zkp1, rkp1x1, vkp1, wkp1, cose, sine)) \
                                  - (self.t3z(qx2, xl2, zk, rkx2, vk, wk, cose, sine)
                                  - self.t3z(qx1, xl1, zk, rkx1, vk, wk, cose, sine))

                            temp[2] = temp[2] + (dx1 + dx2 + dx3) * ndotz
                            temp[3] = temp[3] + (dy1 + dy2 + dy3) * ndotz
                            temp[4] = temp[4] + (dz1 + dz2 + dz3) * ndotz
                            temp[5] = temp[5] + (dx1 + dx2 + dx3) * ndoty
                            temp[6] = temp[6] + (dy1 + dy2 + dy3) * ndoty

                    #  multiply by the density of given prism
                    temp[0] = temp[0] * self.delrho[i70]
                    temp[1] = temp[1] * self.delrho[i70]
                    self.gy[i30, j40] = self.gy[i30, j40] + temp[0]
                    self.gz[i30, j40] = self.gz[i30, j40] + temp[1]

                    if self.do_gradient == 1:
                        temp[2] = temp[2] * self.delrho[i70]
                        temp[3] = temp[3] * self.delrho[i70]
                        temp[4] = temp[4] * self.delrho[i70]
                        temp[5] = temp[5] * self.delrho[i70]
                        temp[6] = temp[6] * self.delrho[i70]
                        self.gzx[i30, j40] = self.gzx[i30, j40] + temp[2]
                        self.gzy[i30, j40] = self.gzy[i30, j40] + temp[3]
                        self.gzz[i30, j40] = self.gzz[i30, j40] + temp[4]
                        self.gyx[i30, j40] = self.gyx[i30, j40] + temp[5]
                        self.gyy[i30, j40] = self.gyy[i30, j40] + temp[6]


        #  add appropriate constants and calculate
        #  gxx gradient by using properties of the field
        self.gy = g * self.gy
        self.gz = g * self.gz
        if self.do_gradient == 1:
            self.gzx = g1 * self.gzx
            self.gzy = g1 * self.gzy
            self.gzz = g1 * self.gzz
            self.gyx = g1 * self.gyx
            self.gyy = g1 * self.gyy
            self.gxx = g1 * self.gxx

        # if you want the average value removed from each
        if self.iave != 1:
            nn = self.nxpts * self.nypts
            agy = np.sum(self.gy) / nn
            agz = np.sum(self.gz) / nn
            self.gy = self.gy - agy
            self.gz = self.gz - agz

            if self.do_gradient == 1:
                agzx = np.sum(self.gzx) / nn
                agzy = np.sum(self.gzy) / nn
                agzz = np.sum(self.gzz) / nn
                agyx = np.sum(self.gyx) / nn
                agyy = np.sum(self.gyy) / nn
                agxx = np.sum(self.gxx) / nn

                self.gzx = self.gzx - agzx
                self.gzy = self.gzy - agzy
                self.gzz = self.gzz - agzz
                self.gyx = self.gyx - agyx
                self.gyy = self.gyy - agyy
                self.gxx = self.gxx - agxx

    # -----------------
    def save_data(self):
        print('Save gravity anomaly and gradients')
        fout = open(self.outfile, 'w')
        if self.do_gradient == 1:
            fout.write('gravity (mGals) and gradient tensor (Eotvos)\n')
            fout.write('x y gz gy gzx gzy gzz gyx gyy gxx\n')
        else:
            fout.write('gravity (mGals) data \n')
            fout.write('x y gz gy \n')

        for i in range(self.nxpts):
            for j in range(self.nypts):
                # Factor 1000 convert (x,y) unit from km to m
                s1 = f'{1000*self.x0[i]:10.2f}{1000*self.y0[j]:10.2f}'
                # gx[i,j] = gy[j,i] if needed
                s2 = f'{self.gz[i,j]:12.4e}{self.gy[i,j]:12.4e}'
                if self.do_gradient == 1:
                    s3 = f'{self.gzx[i,j]:12.4e}{self.gzy[i,j]:12.4e}'
                    s4 = f'{self.gzz[i,j]:12.4e}{self.gyx[i,j]:12.4e}'
                    s5 = f'{self.gyy[i,j]:12.4e}{self.gxx[i,j]:12.4e}\n'
                    fout.write(s1+s2+s3+s4+s5)
                else:
                    fout.write(s1+s2+'\n')

    # ------------------
    def r(self, x, v, w):
        return math.sqrt(x * x + v * v + w * w)

    # ------------------
    def q(self, x, v, w):
        return x * x + x * v + w * w

    # -------------------
    def t1(self, x,v,r):
        if (x+r) > 0.0:
            return v*np.log(x+r)
        else:
            return 0.0
    # -------------------
    def t2(self, x,v,r):
        if (r+v) > 0.0:
            return x*np.log(r+v)
        else:
            return 0.0
    # -------------------
    def t3(self, x,v,w,r):
        pi2 = 2.0 * math.pi
        if abs(w) < cTiny:
            t = 0.0
        elif abs(v) < cTiny:
            arg = (x*r+w*w+x*x)/w
            if abs(arg) < cTiny:
                t = -w * pi2
            else:
                t = -w * pi2 * arg / abs(arg)
        else:
            arg = (x * x + x * r + w * w) / (v * w)
            t = -w * math.atan(arg)

        return t

    # -------------------
    def t1x(self, v,r):
        if (abs(v) < cTiny) and (abs(r) < cTiny):
            return 1.0
        else:
            return -v / r

    # -------------------
    def t2x(self, x,v,r):
        if ((abs(v) < cTiny) and (abs(r) < cTiny)) or (abs(v+r) < cTiny):
            return -1.0
        elif (v+r) < 0.0:
            return -x * x / (r * (v + r))
        else:
            return -(np.log(v+r)+x*x/(r*(v+r)))

    # -------------------
    def t3x(self, q,x,r,v,w):
        if (abs(w) < cTiny) or (abs(v) < cTiny):
            return 0.0
        else:
            return v*w*w*((2.0*x*r+x*x+r*r)/r)/(q*q+v*v*w*w)

    # -------------------
    def t1y(self, x,y,v,r,cose):
        if (abs(x) < cTiny) and (abs(r) < cTiny):
            t = -cose
        elif abs(x + r) < cTiny:
            t = 0.0
        else:
            t = -(cose*np.log(x+r)+(v*y)/(r*(x+r)))

        return t

# -------------------
    def t2y(self, x,y,v,r,cose):
        if (abs(v) < cTiny) and (abs(r) < cTiny):
            t = -cose
        elif abs(v + r) < cTiny:
            t = 0.0
        else:
            t = -(cose+y/r)*(x/(v+r))

        return t

    # -------------------
    def t3y(self, q,x,y,r,v,w,cose,sine):
        piby2 = 2.0 * math.pi
        if (abs(w)<cTiny) and (abs(v)<cTiny) and (abs(q)<cTiny):
            t = -sine * piby2
        elif (abs(w) < cTiny) and (abs(v) < cTiny):
            t = -sine * piby2 * q / abs(q)
        elif (abs(w) < cTiny) and (abs(q) < cTiny):
            t = 0.0
        elif abs(w) < cTiny:
            t = -sine * piby2 * (q / v) / abs(q / v)
        elif abs(v) < cTiny:
            t = -sine*piby2*(q/w)/abs(q/w)-w*w*cose/q
        else:
            t = -sine * math.atan(q / (v * w)) + \
                w * (v * w * x * y / r + v * sine *
                (q - 2.0 * w * w) - w * q * cose) / \
                (q * q + v * v * w * w)

        return t

    # -------------------
    def t1z(self, x,v,z,r,sine):
        if (x > 0.0) and (r > 0.0):
            t = -(sine * np.log(x+r)+v*z/(r*(x+r)))
        elif (abs(x) < cTiny) and (abs(r) < cTiny):
            t = sine
        elif abs(x+r) < cTiny:
            t = 0.0
        else:
            t = -(sine * np.log(x+r)+v*z/(r*(x+r)))

        return t

    # -------------------
    def t2z(self, x,v,z,r,sine):
        if (v > 0.0) and (r > 0.0):
            t = -(sine+z/r)*(x/(v+r))
        elif (abs(v) < cTiny) and (abs(r) < cTiny):
            t = -sine
        elif abs(v+r) < cTiny:
            t = 0.0
        else:
            t = -(sine+z/r)*(x/(v+r))

        return t

    # -------------------
    def t3z(self, q,x,z,r,v,w,cose,sine):
        piby2 = 2.0 * math.pi
        if (abs(w) < cTiny) and (abs(v) < cTiny) and (abs(q) < cTiny):
            t = cose * piby2
        elif (abs(w) < cTiny) and (abs(v) < cTiny):
            t = cose*piby2*q/abs(q)
        elif (abs(w) < cTiny) and (abs(q) < cTiny):
            t = 0.0
        elif abs(w) < cTiny:
            t = cose*piby2*(q/v)/abs(q/v)
        elif abs(v) < cTiny:
            t = cose*piby2*(q/w)/abs(q/w) - w*w*sine/q
        else:
            t = cose*math.atan(q/(v*w)) + w*(v*w*x*z/r-
                v*cose*(q-2.0*w*w)-q*w*sine)/(q*q+v*v*w*w)

        return t

#------------------------
if __name__ == '__main__':
    grav = GravGrad()
    grav.input_density_model()
    grav.compute_gravity()
    grav.save_data()
    print("\nGRAVGRAD ends!")

