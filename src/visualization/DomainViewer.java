package visualization;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.gl2.GLUT;

import objects.Sensor;
import utilities.Constants;
import utilities.Point3f;
import utilities.Point3i;

//================================================================
/**
 * A view that displays OpenGL in a canvas. Draws a dynamically
 * changing source of 3D data.
 *
 * Copyright: Copyright (c) 2010 Wade Walker. Free for any use, but
 * credit is appreciated.
 * 
 * @author Wade Walker
 * @version 1.0
 */
public class DomainViewer {

	private GLCanvas glcanvas;
	private GLContext glcontext;

	private int [] aiVertexBufferIndices = new int [] {-1 };

	private Display display;

	// Current mouse position
	private Point pt;

	private float zPlaneRotation = 0.0f;
	private float xPlaneRotation = 20.0f;
	private float xTranslate = 0.0f;
	private float yTranslate = 0.0f;
	private float zTranslate = 0.0f;
	private float zoom = 1f;

	private DomainVisualization domainVisualization;

	private boolean reset = true;
	private boolean resetConfigurations = true;
	private Map<String, TreeMap<Float, List<Face>>> validFaces;
	private Map<String, TreeMap<Float, List<Face>>> cloudFaces;
	private Map<String, TreeMap<Float, List<Face>>> configurations;
	private List<Line> lines;
	private Point3f cameraPosition = new Point3f(0, 0, 0);

	private int numVertices = 0;

	//================================================================
	/**
	 * Constructor.
	 */
	public DomainViewer(Display display, Composite compositeParent, 
			DomainVisualization domainVisualization) {
		this.display = display;
		this.domainVisualization = domainVisualization;

		Point3f temp = domainVisualization.getRenderDistance();
		float maxDistance = Math.max(temp.getX(), Math.max(temp.getY(), temp.getZ()));
		this.zoom = 9000 / maxDistance;
		
		GLData gldata = new GLData();
		gldata.doubleBuffer = true;
		validFaces = new HashMap<String, TreeMap<Float, List<Face>>>();
		cloudFaces = new HashMap<String, TreeMap<Float, List<Face>>>();
		configurations = new HashMap<String, TreeMap<Float, List<Face>>>();
		lines = new ArrayList<Line>();

		Point3f distance = domainVisualization.getRenderDistance();

		xTranslate = distance.getX()/2;
		yTranslate = distance.getY()/2;
		zTranslate = distance.getZ()/2;		//TODO: Should this be Z? (Was Y when I found it)

		glcanvas = new GLCanvas(compositeParent, SWT.BORDER, gldata);

		glcanvas.setCurrent();
		glcontext = GLDrawableFactory.getFactory( GLProfile.get( GLProfile.GL2 ) ).createExternalGLContext();

		compositeParent.addListener(SWT.MouseVerticalWheel, new Listener() {
			@Override
			public void handleEvent(Event event) {
				zoom-=event.count*0.01;
			}
		});
		
		glcanvas.addListener( SWT.Resize, new Listener() {
			public void handleEvent( Event event ) {
				glcanvas.setCurrent();
				glcontext.makeCurrent();
				GL2 gl = glcontext.getGL().getGL2();
				setTransformsAndViewport( gl );
				glcontext.release();
			}
		});

		glcanvas.addListener(SWT.MouseMove, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if(pt == null)
					return;
				//if(translating) {
				//	xtrans -= (pt.x-event.x)*10;
				//	ytrans += (pt.y-event.y)*10;
				//} else {
				zPlaneRotation -= (pt.x-event.x)*.25;
				xPlaneRotation -= (pt.y-event.y)*.25;
				//}
				pt = new Point(event.x, event.y);
			}
		});

		glcanvas.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				pt = null;
			}			
		});
		
		glcanvas.addListener(SWT.MouseDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				glcanvas.forceFocus();
				pt = new Point(event.x, event.y);
			}
		});

		glcontext.makeCurrent();
		GL2 gl = glcontext.getGL().getGL2();
		gl.setSwapInterval( 1 );	
		gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // set background (clear) color
		gl.glColor3f( 1.0f, 0.0f, 0.0f );
		gl.glClearDepth(1.0);
		gl.glLineWidth(1);
		gl.glEnable(GL.GL_DEPTH_TEST);	
		gl.glDepthFunc(GL2.GL_LESS);  // the type of depth test to do

		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);	

		gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glShadeModel(GL2.GL_SMOOTH); // blends colors nicely, and smoothes out lighting

		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		
		gl.glDisable(GL2.GL_CULL_FACE);

		float ambientLight[] = { 0.2f, 0.2f, 0.2f, 1.0f };
		float diffuseLight[] = { 0.5f, 0.5f, 0.5f, 1.0f };
		float specularLight[] = { 1.0f, 1.0f, 1.0f, 1.0f };	

		// Assign created components to GL_LIGHT0.
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight, 0);
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight, 0);
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight, 0);
		gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, new float[]{0, -20000, 20000, 1.0f}, 0);

		glcontext.release();
		
		// Repaint loop
		DomainViewer.this.display.asyncExec(new Runnable() {
			public void run() {
				if(glcanvas.isVisible() ){
					draw();
				}
				DomainViewer.this.display.asyncExec(this);
			}
		});
	}

	public void reset() {
		this.reset = true;
	}

	public void resetConfigurations() {
		this.resetConfigurations = true;
	}
	
	public void show(){
		glcanvas.setVisible(true);
	}
	
	public void hide(){
		glcanvas.setVisible(false);
	}


	private void draw() {
		if(glcanvas != null && !glcanvas.isDisposed()) {
			glcanvas.setCurrent();
			glcontext.makeCurrent();

			GL2 gl2 = glcontext.getGL().getGL2();
			setTransformsAndViewport( gl2 );

			gl2.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
			gl2.glClearColor( 1.0f, 1.0f, 1.0f, 1.0f );

			//TODO: TEST!
			domainVisualization.checkSelectAll();
			
			if(reset) {
				this.buildLines();
				this.buildValidFaces();
				this.buildCloudFaces();
				reset = false;
			}			
			if(resetConfigurations) {
				this.buildSensors();
				resetConfigurations = false;
			}

			createAndFillVertexBuffer(gl2);

			// draw all quads in vertex buffer
			gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0] );
			gl2.glEnableClientState( GL2.GL_VERTEX_ARRAY );
			gl2.glEnableClientState( GL2.GL_COLOR_ARRAY );
			gl2.glVertexPointer( 4, GL.GL_FLOAT, 8 * Buffers.SIZEOF_FLOAT, 0 );
			gl2.glColorPointer( 4, GL.GL_FLOAT, 8 * Buffers.SIZEOF_FLOAT, 4 * Buffers.SIZEOF_FLOAT );
			
			// needed so material for quads will be set from color map
			gl2.glEnable( GL2.GL_COLOR_MATERIAL );
			gl2.glColorMaterial( GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE );
			gl2.glDisable(GL2.GL_LIGHTING);
			
			if(domainVisualization.drawMesh()) {

				gl2.glColor3f(1f, .4f, .4f);
				gl2.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_LINE );
				gl2.glDrawArrays( GL2.GL_LINES, 0, lines.size()*2 );

				GLUT glut = new GLUT();
				List<Float> xs = domainVisualization.getRenderCellBoundsX();
				List<Float> ys = domainVisualization.getRenderCellBoundsY();
				List<Float> zs = domainVisualization.getRenderCellBoundsZ();
				List<Float> labelX = domainVisualization.getTrueCellBoundsX();
				List<Float> labelY = domainVisualization.getTrueCellBoundsY();
				List<Float> labelZ = domainVisualization.getTrueCellBoundsZ();
				gl2.glPushMatrix();
				gl2.glColor3f(0, 0, 0);

				// TODO: Scale these values based on domain size
				Point3f axisLengths = domainVisualization.getRenderDistance();
				float maxLength = Math.max(axisLengths.getX(), Math.max(axisLengths.getY(), axisLengths.getZ()));
				for(int i = 0; i < xs.size(); i += domainVisualization.getTickX()) {
					gl2.glRasterPos3f(xs.get(i), -maxLength/30, -maxLength/30);
					glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, Constants.decimalFormat.format(labelX.get(i)));
				}
				for(int i = 0; i < ys.size(); i += domainVisualization.getTickY()) {
					gl2.glRasterPos3f(xs.get(xs.size()-1)+maxLength/30, ys.get(i), -maxLength/30);
					glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, Constants.decimalFormat.format(labelY.get(i)));
				}
				for(int i = domainVisualization.getTickZ(); i < zs.size(); i += domainVisualization.getTickZ()) {
					gl2.glRasterPos3f(-maxLength/20, -maxLength/20, zs.get(i));
					glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, Constants.decimalFormat.format(labelZ.get(i)));
				}
				// User defined labels: TODO: offset with width
				gl2.glRasterPos3f(xs.get(xs.size()-1)/2, -maxLength/10, -maxLength/10);
				glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, domainVisualization.getXLabel());

				gl2.glRasterPos3f(xs.get(xs.size()-1)+maxLength/10, ys.get(ys.size()-1)/2, -maxLength/10);
				glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, domainVisualization.getYLabel());

				gl2.glRasterPos3f(-maxLength/10, -maxLength/10, zs.get(zs.size()-1)/2);
				glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, domainVisualization.getZLabel());

				gl2.glPopMatrix();
			}
			
			gl2.glEnable(GL2.GL_LIGHTING);
			
			int numMeshVertices = lines.size()*2;
			int numValidFaces = 0;
			for(String key: validFaces.keySet()) {
				if(domainVisualization.renderValid(key)) {
					for(Float distance: validFaces.get(key).keySet()) {
						numValidFaces += validFaces.get(key).get(distance).size();
					}
				}
			}
			numValidFaces *= 4;
			int numCloudFaces = 0;
			for(String key: cloudFaces.keySet()) {
				if(domainVisualization.renderCloud(key)) {
					for(Float distance: cloudFaces.get(key).keySet()) {
						numCloudFaces += cloudFaces.get(key).get(distance).size();
					}
				}
			}
			numCloudFaces *= 4;

			gl2.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_FILL );
			gl2.glDrawArrays( GL2.GL_QUADS, numMeshVertices+numValidFaces+numCloudFaces, numVertices);

			gl2.glDepthMask(false);
			gl2.glPolygonMode( GL.GL_FRONT_AND_BACK, GL2.GL_FILL );
			gl2.glDrawArrays( GL2.GL_QUADS, numMeshVertices, numMeshVertices+numValidFaces+numCloudFaces);
			gl2.glDepthMask(true); // Already sorted, don't override
			
			
		
			//Luke's floating axis thing
			// Draw axis
			GLUT glut = new GLUT();
			gl2.glPushMatrix();
			gl2.glLoadIdentity();
			gl2.glTranslatef(-6000.0f, 0.0f, -6000f);		
			gl2.glRotatef(zPlaneRotation, 0.0f, 0.0f, 1.0f); // z plane
			gl2.glRotatef(xPlaneRotation, 1.0f, 0.0f, 0.0f); // y plane
			gl2.glLineWidth(2);
			gl2.glColor3f(1.0f, 0.0f, 0.0f);
			gl2.glRasterPos3f(2000f, 0f, 0f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, "X");
			gl2.glBegin(GL.GL_LINES);
			gl2.glVertex3f(0.0f, 0.0f, 0.0f);
			gl2.glVertex3f(2000.0f, 0.0f, 0.0f);
			gl2.glEnd();
			gl2.glColor3f(0.0f, 1.0f, 0.0f);
			gl2.glRasterPos3f(0f, 2000f, 0f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, "Y");
			gl2.glBegin(GL.GL_LINES);
			gl2.glVertex3f(0.0f, 0.0f, 0.0f);
			gl2.glVertex3f(0.0f, 2000.0f, 0.0f);
			gl2.glEnd();
			gl2.glColor3f(0.0f, 0.0f, 1.0f);
			gl2.glRasterPos3f(0f, 0f, 2000f);
			glut.glutBitmapString(GLUT.BITMAP_HELVETICA_10, "Z");
			gl2.glBegin(GL.GL_LINES);
			gl2.glVertex3f(0.0f, 0.0f, 0.0f);
			gl2.glVertex3f(0.0f, 0.0f, 2000.0f);
			gl2.glRasterPos3f(0f,0f,0f);
			gl2.glEnd();
			//grab the rotation
			// Reset
			gl2.glLineWidth(1);
			gl2.glPopMatrix();
			
			
			// disable arrays once we're done
			gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, 0 );
			gl2.glDisableClientState( GL2.GL_VERTEX_ARRAY );
			gl2.glDisableClientState( GL2.GL_COLOR_ARRAY );
			gl2.glDisable( GL2.GL_COLOR_MATERIAL );

			glcanvas.swapBuffers();
			glcontext.release();
		}
	}

	public void setLayoutData(GridData layoutData) {
		glcanvas.setLayoutData(layoutData);
	}

	//================================================================
	/**
	 * Sets up an orthogonal projection suitable for a 2D CAD program.
	 *
	 * @param gl GL object to set transforms and viewport on.
	 */
	protected void setTransformsAndViewport( GL2 gl2 ) {

		Rectangle rectangle = glcanvas.getClientArea();
		int iWidth = rectangle.width;
		int iHeight = Math.max( rectangle.height, 1 );

		gl2.glMatrixMode( GLMatrixFunc.GL_PROJECTION );
		gl2.glLoadIdentity();

		// set the clipping planes based on the ratio of object units
		// to screen pixels, but preserving the correct aspect ratio

		GLU glu = new GLU();
		glu.gluPerspective(45f, iWidth/iHeight, 0.1f, 100000.0f);
		
		// camera position, look at position, up vector
		glu.gluLookAt(0, -20000, 0, 0, 0, 0, 0, 0, 1);


		gl2.glMatrixMode( GLMatrixFunc.GL_MODELVIEW );
		gl2.glViewport( 0, 0, iWidth, iHeight );
				
		gl2.glLoadIdentity();	

		gl2.glScalef(zoom, zoom, zoom);
		
		gl2.glRotatef(zPlaneRotation, 0.0f, 0.0f, 1.0f); // z plane
		gl2.glRotatef(xPlaneRotation, 1.0f, 0.0f, 0.0f); // y plane
		
		gl2.glTranslatef( xTranslate, yTranslate, zTranslate);
		
		float[] matModelView = new float[16];
		gl2.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, matModelView, 0);
//		System.out.println(Arrays.toString(matModelView));
		cameraPosition = new Point3f(-matModelView[12], -matModelView[13], -matModelView[14]);
//		System.out.println(cameraPosition);
	}

	protected void createAndFillVertexBuffer(GL2 gl2) {
		int numLines = lines.size();
		int numFaces = 0;
		// Max to min distance
		Map<Float, List<Face>> facesToDraw = new TreeMap<Float, List<Face>>(Collections.reverseOrder());
		Map<Float, List<Face>> configurationsToDraw = new TreeMap<Float, List<Face>>(Collections.reverseOrder());
		for(String key: validFaces.keySet()) {
			if(domainVisualization.renderValid(key)) {
				for(Float distance: validFaces.get(key).keySet()) {
					numFaces += validFaces.get(key).get(distance).size();
					if(!facesToDraw.containsKey(distance)) {
						facesToDraw.put(distance, new ArrayList<Face>());
					}
					for(Face face:validFaces.get(key).get(distance))
						facesToDraw.get(distance).add(face);
				}
			}
		}
		for(String key: cloudFaces.keySet()) {
			if(domainVisualization.renderCloud(key)) {
				for(Float distance: cloudFaces.get(key).keySet()) {
					numFaces += cloudFaces.get(key).get(distance).size();
					if(!facesToDraw.containsKey(distance)) {
						facesToDraw.put(distance, new ArrayList<Face>());
					}
					for(Face face:cloudFaces.get(key).get(distance))
						facesToDraw.get(distance).add(face);
				}
			}
		}
		for(String key: configurations.keySet()) {
			if(domainVisualization.renderConfiguration(key)) {
				for(Float distance: configurations.get(key).keySet()) {
					numFaces += configurations.get(key).get(distance).size();
					if(!configurationsToDraw.containsKey(distance)) {
						configurationsToDraw.put(distance, new ArrayList<Face>());
					}
					for(Face face:configurations.get(key).get(distance))
						configurationsToDraw.get(distance).add(face);
				}
			}
		}		
		numVertices = numLines*2 + numFaces*4;

		if( aiVertexBufferIndices[0] != -1 ) {
			gl2.glDeleteBuffers( 1, aiVertexBufferIndices, 0 );
		}
		// check for VBO support
		if(    !gl2.isFunctionAvailable( "glGenBuffers" )
				|| !gl2.isFunctionAvailable( "glBindBuffer" )
				|| !gl2.isFunctionAvailable( "glBufferData" )
				|| !gl2.isFunctionAvailable( "glDeleteBuffers" ) ) {
			System.err.println("Vertex buffer objects not supported.");
		}

		gl2.glGenBuffers( 1, aiVertexBufferIndices, 0 );

		// create vertex buffer data store without initial copy
		gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0] );
		gl2.glBufferData( GL.GL_ARRAY_BUFFER,
				numVertices * 4 * Buffers.SIZEOF_FLOAT * 2,
				null,
				GL2.GL_DYNAMIC_DRAW );

		// map the buffer and write vertex and color data directly into it
		gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, aiVertexBufferIndices[0] );
		ByteBuffer bytebuffer = gl2.glMapBuffer( GL.GL_ARRAY_BUFFER, GL2.GL_WRITE_ONLY );
		FloatBuffer floatBuffer = bytebuffer.order( ByteOrder.nativeOrder() ).asFloatBuffer();

		// Fill the float buffer
		for(Line line: lines) {
			bufferLine(floatBuffer, line.v1, line.v2, line.color);
		}		

		for(List<Face> faces: facesToDraw.values()) {
			for(Face face: faces) {
				bufferQuad(floatBuffer, face.v1, face.v2, face.v3, face.v4, face.color, face.transparency);
			}
		}

		for(List<Face> faces: configurationsToDraw.values()) {
			for(Face face: faces) {
				bufferQuad(floatBuffer, face.v1, face.v2, face.v3, face.v4, face.color, face.transparency);
			}
		}


		gl2.glUnmapBuffer( GL.GL_ARRAY_BUFFER );	
	}

	protected void bufferLine(FloatBuffer floatBuffer, Point3f v1, Point3f v2, Point3i color) {
		floatBuffer.put(v1.getX());
		floatBuffer.put(v1.getY());
		floatBuffer.put(v1.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(1);

		floatBuffer.put(v2.getX());
		floatBuffer.put(v2.getY());
		floatBuffer.put(v2.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(1);
	}

	protected void bufferQuad(FloatBuffer floatBuffer, Point3f v1, Point3f v2, 
			Point3f v3, Point3f v4, Point3i color, float transparency) {

		floatBuffer.put(v1.getX());
		floatBuffer.put(v1.getY());
		floatBuffer.put(v1.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(transparency);

		floatBuffer.put(v2.getX());
		floatBuffer.put(v2.getY());
		floatBuffer.put(v2.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(transparency);

		floatBuffer.put(v3.getX());
		floatBuffer.put(v3.getY());
		floatBuffer.put(v3.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(transparency);

		floatBuffer.put(v4.getX());
		floatBuffer.put(v4.getY());
		floatBuffer.put(v4.getZ());
		floatBuffer.put(1);

		floatBuffer.put(color.getI()/255.0f);
		floatBuffer.put(color.getJ()/255.0f);
		floatBuffer.put(color.getK()/255.0f);
		floatBuffer.put(transparency);	
	}

	private void buildLines() {
		List<Float> xs = domainVisualization.getRenderCellBoundsX();
		List<Float> ys = domainVisualization.getRenderCellBoundsY();
		List<Float> zs = domainVisualization.getRenderCellBoundsZ();

		Point3f distance = domainVisualization.getRenderDistance();
		Point3i meshColor = domainVisualization.getMeshColor();
		List<Line> meshLines = new ArrayList<Line>();

		// Bottom; z == 0
		for(int i = 0; i < xs.size(); i++) {
			meshLines.add(new Line(new Point3f(xs.get(i), 0, 0), 
					new Point3f(xs.get(i), ys.get(ys.size()-1), 0), 
					meshColor)); 
			// y's across the x axis
			for(int j = 0; j < ys.size(); j++) {
				meshLines.add(new Line(new Point3f(0, ys.get(j), 0), 
						new Point3f(xs.get(xs.size()-1), ys.get(j), 0), 
						meshColor));
			}
		}
		// Left; x == 0
		for(int i = 0; i < ys.size(); i++) {
			meshLines.add(new Line(new Point3f(0, ys.get(i), 0), 
					new Point3f(0, ys.get(i), zs.get(zs.size()-1)), 
					meshColor)); 
			// z's across the y axis
			for(int j = 0; j < zs.size(); j++) {
				meshLines.add(new Line(new Point3f(xs.get(0), 0, zs.get(j)), 
						new Point3f(xs.get(0), ys.get(ys.size()-1), zs.get(j)), 
						meshColor)); 
			}
		}	
		// Back; y == 0
		for(int i = 0; i < zs.size(); i++) {
			meshLines.add(new Line(new Point3f(0, ys.get(ys.size()-1), zs.get(i)), 
					new Point3f(xs.get(xs.size()-1), ys.get(ys.size()-1), zs.get(i)), 
					meshColor)); 
			// x's across the z axis
			for(int j = 0; j < xs.size(); j++) {
				meshLines.add(new Line(new Point3f(xs.get(j), ys.get(ys.size()-1), 0), 
						new Point3f(xs.get(j), ys.get(ys.size()-1), zs.get(zs.size()-1)), 
						meshColor)); 
			}
		}			

		xTranslate = -distance.getX()/2;
		yTranslate = -distance.getY()/2;
		zTranslate = -distance.getZ()/2;

		synchronized(lines) {
			lines.clear();
			lines.addAll(meshLines);
		}
	}

	private void buildValidFaces() {
		List<Float> xs = domainVisualization.getRenderCellBoundsX();
		List<Float> ys = domainVisualization.getRenderCellBoundsY();
		List<Float> zs = domainVisualization.getRenderCellBoundsZ();
		Point3f cameara = cameraPosition; // TODO: Doesn't seem to change anything
		Map<String, TreeMap<Float, List<Face>>> facesByDistance = new HashMap<String, TreeMap<Float, List<Face>>>();
		
		//Get all the ones for the valid nodes
		for(String sensor: domainVisualization.getAllValidsToRender()) {
			Point3i color = domainVisualization.getColorOfValid(sensor);
			float transparency = domainVisualization.getValidTransparency(sensor);
			List<Point3i> nodes = domainVisualization.getValidNodes(sensor);
			if(!facesByDistance.containsKey(sensor))
				facesByDistance.put(sensor, new TreeMap<Float, List<Face>>());
			for(Point3i point: nodes) {
				float xMin = xs.get(point.getI()-1);
				float yMin = ys.get(point.getJ()-1);
				float zMin = zs.get(point.getK()-1);
				float xMax = xs.get(point.getI());
				float yMax = ys.get(point.getJ());
				float zMax = zs.get(point.getK());
				Face f1 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMax, zMin), new Point3f(xMin, yMax, zMin), color, transparency);
				Face f2 = new Face(new Point3f(xMin, yMin, zMax), new Point3f(xMax, yMin, zMax), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);
				Face f3 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMin, yMax, zMin), 
						new Point3f(xMin, yMax, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f4 = new Face(new Point3f(xMax, yMin, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMax, yMin, zMax), color, transparency);
				Face f5 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMin, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f6 = new Face(new Point3f(xMin, yMax, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);

				float f1Distance = f1.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f1Distance)) {
					facesByDistance.get(sensor).put(f1Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f1Distance).add(f1);

				float f2Distance = f2.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f2Distance)) {
					facesByDistance.get(sensor).put(f2Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f2Distance).add(f2);

				float f3Distance = f3.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f3Distance)) {
					facesByDistance.get(sensor).put(f3Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f3Distance).add(f3);

				float f4Distance = f4.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f4Distance)) {
					facesByDistance.get(sensor).put(f4Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f4Distance).add(f4);

				float f5Distance = f5.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f5Distance)) {
					facesByDistance.get(sensor).put(f5Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f5Distance).add(f5);

				float f6Distance = f6.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f6Distance)) {
					facesByDistance.get(sensor).put(f6Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f6Distance).add(f6);
			}
		}

		// Purge equal faces

		for(String sensor: facesByDistance.keySet()) {
			for(Float distance: facesByDistance.get(sensor).keySet()) {
				Map<Face, Integer> faceCount = new HashMap<Face, Integer>();
				for(Face face: facesByDistance.get(sensor).get(distance)) {
					Face found = null;
					for(Face countedFace: faceCount.keySet()) {
						if(face.equals(countedFace)) {
							found = countedFace;
							break;
						}
					}
					if(found != null){
						int currentCount = faceCount.get(found);
						faceCount.put(found, currentCount + 1);
					} else {
						faceCount.put(face, 1);
					}
				}
				facesByDistance.get(sensor).get(distance).clear();
				for(Face face: faceCount.keySet()) {
					if(faceCount.get(face) == 1)
						facesByDistance.get(sensor).get(distance).add(face);
				}
			}
		}


		synchronized(validFaces) {
			validFaces.clear();
			validFaces.putAll(facesByDistance);
		}
	}

	private void buildCloudFaces() {
		List<Float> xs = domainVisualization.getRenderCellBoundsX();
		List<Float> ys = domainVisualization.getRenderCellBoundsY();
		List<Float> zs = domainVisualization.getRenderCellBoundsZ();
		Point3f cameara = cameraPosition; // TODO: Doesn't seem to change anything
		Map<String, TreeMap<Float, List<Face>>> facesByDistance = new HashMap<String, TreeMap<Float, List<Face>>>();
		
		//Get all the ones for the cloud nodes
		for(String sensor: domainVisualization.getAllCloudsToRender()) {
			Point3i color = domainVisualization.getColorOfCloud(sensor);
			float transparency = domainVisualization.getCloudTransparency(sensor);
			List<Point3i> nodes = domainVisualization.getCloudNodes(sensor);
			if(!facesByDistance.containsKey(sensor))
				facesByDistance.put(sensor, new TreeMap<Float, List<Face>>());
			for(Point3i point: nodes) {
				float xMin = xs.get(point.getI()-1);
				float yMin = ys.get(point.getJ()-1);
				float zMin = zs.get(point.getK()-1);
				float xMax = xs.get(point.getI());
				float yMax = ys.get(point.getJ());
				float zMax = zs.get(point.getK());
				Face f1 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMax, zMin), new Point3f(xMin, yMax, zMin), color, transparency);
				Face f2 = new Face(new Point3f(xMin, yMin, zMax), new Point3f(xMax, yMin, zMax), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);
				Face f3 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMin, yMax, zMin), 
						new Point3f(xMin, yMax, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f4 = new Face(new Point3f(xMax, yMin, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMax, yMin, zMax), color, transparency);
				Face f5 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMin, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f6 = new Face(new Point3f(xMin, yMax, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);

				float f1Distance = f1.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f1Distance)) {
					facesByDistance.get(sensor).put(f1Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f1Distance).add(f1);

				float f2Distance = f2.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f2Distance)) {
					facesByDistance.get(sensor).put(f2Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f2Distance).add(f2);

				float f3Distance = f3.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f3Distance)) {
					facesByDistance.get(sensor).put(f3Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f3Distance).add(f3);

				float f4Distance = f4.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f4Distance)) {
					facesByDistance.get(sensor).put(f4Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f4Distance).add(f4);

				float f5Distance = f5.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f5Distance)) {
					facesByDistance.get(sensor).put(f5Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f5Distance).add(f5);

				float f6Distance = f6.getDistance(cameara);
				if(!facesByDistance.get(sensor).containsKey(f6Distance)) {
					facesByDistance.get(sensor).put(f6Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(sensor).get(f6Distance).add(f6);
			}
		}

		// Purge equal faces

		for(String sensor: facesByDistance.keySet()) {
			for(Float distance: facesByDistance.get(sensor).keySet()) {
				Map<Face, Integer> faceCount = new HashMap<Face, Integer>();
				for(Face face: facesByDistance.get(sensor).get(distance)) {
					Face found = null;
					for(Face countedFace: faceCount.keySet()) {
						if(face.equals(countedFace)) {
							found = countedFace;
							break;
						}
					}
					if(found != null){
						int currentCount = faceCount.get(found);
						faceCount.put(found, currentCount + 1);
					} else {
						faceCount.put(face, 1);
					}
				}
				facesByDistance.get(sensor).get(distance).clear();
				for(Face face: faceCount.keySet()) {
					if(faceCount.get(face) == 1)
						facesByDistance.get(sensor).get(distance).add(face);
				}
			}
		}


		synchronized(cloudFaces) {
			cloudFaces.clear();
			cloudFaces.putAll(facesByDistance);
		}
	}

	private void buildSensors() {
		List<Float> xs = domainVisualization.getRenderCellBoundsX();
		List<Float> ys = domainVisualization.getRenderCellBoundsY();
		List<Float> zs = domainVisualization.getRenderCellBoundsZ();
		Point3f cameara = new Point3f(0, -20000, 0); 
		Map<String, TreeMap<Float, List<Face>>> facesByDistance = new HashMap<String, TreeMap<Float, List<Face>>>();
		for(String configUUID: domainVisualization.getAllConfigurationsToRender()) {
			List<Sensor> nodes = new ArrayList<Sensor>(domainVisualization.getSensorsInConfiguration(configUUID));
			if(!facesByDistance.containsKey(configUUID))
				facesByDistance.put(configUUID, new TreeMap<Float, List<Face>>());
			for(Sensor sensor: nodes) {
				if(!domainVisualization.renderSensor(sensor.getSensorType())) continue;
				float transparency = domainVisualization.getSensorTransparency(sensor.getSensorType());
				Point3i color = domainVisualization.getColorOfSensor(sensor.getSensorType());
				color = new Point3i((int)(color.getI()*.8), (int)(color.getJ()*.8), (int)(color.getK()*.8));
				float xMin = xs.get(sensor.getIJK().getI()-1);
				float yMin = ys.get(sensor.getIJK().getJ()-1);
				float zMin = zs.get(sensor.getIJK().getK()-1);
				float xMax = xs.get(sensor.getIJK().getI());
				float yMax = ys.get(sensor.getIJK().getJ());
				float zMax = zs.get(sensor.getIJK().getK());
				Face f1 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMax, zMin), new Point3f(xMin, yMax, zMin), color, transparency);
				Face f2 = new Face(new Point3f(xMin, yMin, zMax), new Point3f(xMax, yMin, zMax), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);
				Face f3 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMin, yMax, zMin), 
						new Point3f(xMin, yMax, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f4 = new Face(new Point3f(xMax, yMin, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMax, yMin, zMax), color, transparency);
				Face f5 = new Face(new Point3f(xMin, yMin, zMin), new Point3f(xMax, yMin, zMin), 
						new Point3f(xMax, yMin, zMax), new Point3f(xMin, yMin, zMax), color, transparency);
				Face f6 = new Face(new Point3f(xMin, yMax, zMin), new Point3f(xMax, yMax, zMin), 
						new Point3f(xMax, yMax, zMax), new Point3f(xMin, yMax, zMax), color, transparency);

				float f1Distance = f1.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f1Distance)) {
					facesByDistance.get(configUUID).put(f1Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f1Distance).add(f1);

				float f2Distance = f2.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f2Distance)) {
					facesByDistance.get(configUUID).put(f2Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f2Distance).add(f2);

				float f3Distance = f3.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f3Distance)) {
					facesByDistance.get(configUUID).put(f3Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f3Distance).add(f3);

				float f4Distance = f4.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f4Distance)) {
					facesByDistance.get(configUUID).put(f4Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f4Distance).add(f4);

				float f5Distance = f5.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f5Distance)) {
					facesByDistance.get(configUUID).put(f5Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f5Distance).add(f5);

				float f6Distance = f6.getDistance(cameara);
				if(!facesByDistance.get(configUUID).containsKey(f6Distance)) {
					facesByDistance.get(configUUID).put(f6Distance, new ArrayList<Face>());
				}				
				facesByDistance.get(configUUID).get(f6Distance).add(f6);
			}
		}


		synchronized(configurations) {
			configurations.clear();
			configurations.putAll(facesByDistance);
		}

	}

	private class Line {
		private Point3f v1, v2;
		private Point3i color;
		public Line(Point3f v1, Point3f v2, Point3i color) {
			this.v1 = v1;
			this.v2 = v2;
			this.color = color;
		}		
	}

	private class Face {
		private Point3f v1, v2, v3, v4;
		private Point3i color;
		private float transparency;
		public Face(Point3f v1, Point3f v2, Point3f v3, Point3f v4, Point3i color, float transparency) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
			this.v4 = v4;
			this.color = color;
			this.transparency = transparency;
		}

		public float getDistance(Point3f pt) {
			Point3f average = new Point3f((v1.getX() + v2.getX() + v3.getX() + v4.getX())/4,
					(v1.getY() + v2.getY() + v3.getY() + v4.getY())/4,
					(v1.getZ() + v2.getZ() + v3.getZ() + v4.getZ())/4);

			return (float)Math.sqrt(
					Math.pow(pt.getX() - average.getX(), 2)
					+ Math.pow(pt.getY() - average.getY(), 2) +
					Math.pow(pt.getZ()-  average.getZ(), 2));
		}

		public boolean equals(Face face) {
			int matches = 0;
			for(Point3f vs: new Point3f[]{v1, v2, v3, v4}) {
				for(Point3f vs2: new Point3f[]{face.v1, face.v2, face.v3, face.v4}) {
					if(vs.equals(vs2)) {
						matches++;
						break;
					}
				}	
			}			
			return matches == 4 && 
					color.equals(face.color) &&
					Float.compare(transparency, face.transparency) == 0;
		}
	}

	//================================================================
	/**
	 * Deletes the vertex and color buffers.
	 */
	protected void disposeVertexBuffers() {
		glcontext.makeCurrent();
		GL2 gl2 = glcontext.getGL().getGL2();
		gl2.glDeleteBuffers( 1, aiVertexBufferIndices, 0 );
		aiVertexBufferIndices[0] = -1;
		glcontext.release();
	}

	public void dispose() {
		disposeVertexBuffers();
		glcanvas.dispose();
	}

}
