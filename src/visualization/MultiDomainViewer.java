package visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import objects.ExtendedConfiguration;
import objects.ScenarioSet;
import objects.ExtendedSensor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;


public class MultiDomainViewer {

	public enum SensorConfigType { INITIAL, NEW, CURRENT, BEST };

	public Map<SensorConfigType, DisplayTool> displayTools; // For calling update functions
	Shell shell;

	public MultiDomainViewer(Display display, ScenarioSet set) {
		displayTools = new HashMap<SensorConfigType, DisplayTool>();
		shell = new Shell(display, SWT.DIALOG_TRIM | SWT.MODELESS); 
		shell.setText("3D Multi-Domain Viewer"); 
		shell.setLayout(new FillLayout());
		Composite comp = new Composite(shell, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		comp.setLayout(gridLayout);
		GLData data = new GLData ();
		data.doubleBuffer = true;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;

		Label label1 = new Label(comp, SWT.NULL);
		label1.setText("Initial Configuration");				
		Label label2 = new Label(comp, SWT.NULL);
		label2.setText("Best Configuration");

		DisplayTool tool1 = new DisplayTool(SensorConfigType.INITIAL, comp, SWT.BORDER, data, set);
		tool1.setLayoutData(gridData);
		displayTools.put(SensorConfigType.INITIAL, tool1);

		DisplayTool tool4 = new DisplayTool(SensorConfigType.BEST, comp,  SWT.BORDER, data, set);
		tool4.setLayoutData(gridData);
		displayTools.put(SensorConfigType.BEST, tool4);

		Label label3 = new Label(comp, SWT.NULL);
		label3.setText("New Configuration");
		Label label4 = new Label(comp, SWT.NULL);
		label4.setText("Current Configuration");	

		DisplayTool tool2 = new DisplayTool(SensorConfigType.NEW, comp,  SWT.BORDER, data, set);
		tool2.setLayoutData(gridData);
		displayTools.put(SensorConfigType.NEW, tool2);	

		DisplayTool tool3 = new DisplayTool(SensorConfigType.CURRENT, comp,  SWT.BORDER, data, set);
		tool3.setLayoutData(gridData);
		displayTools.put(SensorConfigType.CURRENT, tool3);

		shell.setSize(900, 720);
		shell.open();

		displayTools.get(SensorConfigType.CURRENT).updateDisplay();	// Update all of them each iteration?
		displayTools.get(SensorConfigType.NEW).updateDisplay();	
		displayTools.get(SensorConfigType.BEST).updateDisplay();	
		displayTools.get(SensorConfigType.INITIAL).updateDisplay();	

		shell.addListener(SWT.Close, new Listener() 
		{ 
			@Override 
			public void handleEvent(Event event) 
			{ 
				shell.dispose(); 
			} 
		}); 
	}

	public Shell getShell() {
		return shell;
	}

	ExtendedConfiguration initialConfiguration;
	ExtendedConfiguration newConfiguration;
	ExtendedConfiguration bestConfiguration;
	ExtendedConfiguration currentConfiguration;


	public void assignConfigurations(ExtendedConfiguration initialConfiguration, ExtendedConfiguration newConfiguration, ExtendedConfiguration bestConfiguration, ExtendedConfiguration currentConfiguration) {
		this.initialConfiguration = initialConfiguration;
		this.bestConfiguration = bestConfiguration;
		this.currentConfiguration = currentConfiguration;
		this.newConfiguration = newConfiguration;
	}

	public void setConfiguration(ExtendedConfiguration configuration) {
		this.initialConfiguration = configuration;
	}
	/*
	public void updateDisplayTool(final SensorConfigType type, final Configuration configuration) {
		//	displayTools.get(type).update();
		final DisplayTool tool = displayTools.get(type);
		tool.setCurrent();
		tool.getDisplay().asyncExec(new Runnable () {
			public void run () {
				if (!tool.isDisposed()) {
					tool.updateSensors(configuration.getSensors());
				}
			}
		});
	}*/

	public class DisplayTool extends GLCanvas {

		private float zoom = 0;

		private float xrot = -260;
		private float yrot = -110;

		private float xtrans = 0;
		private float ytrans = 0;

		private int xBound = 1000;
		private int yBound = 1000;

		private int lookat = 0;

		private Point pt;
		GLContext context;

		private Map<String, List<Vector3f[]>> nodesByType;
		private Map<String, List<Vector3f[]>> cloudsByType;

		//		private List<Vector3f[]> co2Nodes; // red <- almost solid
		//		private List<Vector3f[]> pressureNodes; // blue <- almost solid 

		//		private List<Vector3f[]> cloudCo2; // red
		//		private List<Vector3f[]> cloudPresure; // blue
		//		private List<Vector3f[]> cloudMix; // purple

		/*************************************************
		 * Information about the run
		 */
		ScenarioSet scenarioSet;


		/**************************************************
		 * Copied form c# version
		 */
		public int colorIndex;

		public int uniform = 2;    // 0=nonuniform, 1=uniform in axis direction, 2=uniform in all directions

		private XYZ time = new XYZ(0, 0, 0);    // To toggle on and off the mesh (right click)
		private XYZ previousMouse = new XYZ(0, 0, 0);   // previous mouse location
		private XYZ rotate = new XYZ(5, 0, 0);   // rotate by x, y z
		private XYZ offset = new XYZ(0, 0, 0);
		private XYZ translate = new XYZ(0, 0, 0); // Because the center may  not be 0, 0, 0
		private XYZ deltaXYZ = new XYZ(0, 0, 0);
		private XYZ keyTranslate = new XYZ(0, 0, 0);
		private XYZ distanceXYZ = new XYZ(0, 0, 0);
		Map<Integer, Integer> cloudNodes;

		private boolean translating = false; 
		private boolean cloud = false;
		Matrix4f currentRotation = new Matrix4f();
		Matrix4f currentTranslation = new Matrix4f();   

		private XYZ minimum = new XYZ(0, 0, 0); // Same in all cases
		private XYZ maximum = new XYZ(0, 0, 0); // same for non uniform and uniform in axis directions

		private XYZ maxXYZUniform = new XYZ(0, 0, 0);

		private IJK nXYZ = new IJK(0, 0, 0);
		float maxDelta = 0;

		private List<Float> cellBoundsX;
		private List<Float> cellBoundsY;
		private List<Float> cellBoundsZ;

		private boolean showMesh = false;   // Mesh toggle
		private boolean rotating = false;
		private boolean drawingLock = false;   // Don't update the sensor list until we're done drawing the previous ones
		private boolean sensorChanged = false; // To keep the timer from updating the visualization too often

		private List<ExtendedSensor> sensors = new ArrayList<ExtendedSensor>();
		private Map<Integer, ExtendedSensor> sensorsMap;
		private Map<Integer, Color> colors;
		private Map<String, Color> colorsByType;
		private boolean colorByType = true; // Will color each cloud differently

		private class XYZ {
			public float x, y, z;
			public XYZ(float x, float y, float z)
			{
				this.x = x;
				this.y = y;
				this.z = z;
			}
		}

		private class IJK {
			public int i, j, k;
			public IJK(int i, int j, int k)
			{
				this.i = i;
				this.j = j;
				this.k = k;
			}
		}

		private XYZ[] Vertices = new XYZ[] {
				new XYZ(0.0f, 0.0f, 0.0f), new XYZ(1.0f, 0.0f, 0.0f), 
				new XYZ(0.0f, 1.0f, 0.0f), new XYZ(1.0f, 1.0f, 0.0f), 
				new XYZ(0.0f, 0.0f, 1.0f), new XYZ(1.0f, 0.0f, 1.0f), 
				new XYZ(0.0f, 1.0f, 1.0f), new XYZ(1.0f, 1.0f, 1.0f)
		};
		//	       6b---------7a
		//        /|         /|
		//		 / |        / |
		//      4d----------5c--3e
		//      | /2f      | /
		//		|/         |/	
		// 		0h----------1g
		//	3, 2, 6, 7,	// Back
		//	5, 4, 0, 1, // Front
		//	7, 6, 4, 5,	// Top
		//	1, 0, 2, 3,	// Bottom
		///	6, 2, 0, 4, // Left
		//	3, 7, 5, 1  // Right
		private XYZ[] Normals = new XYZ[] {
				new XYZ( 0.0f, 1.0f,  0.0f), new XYZ( 0.0f,  -1.0f, 0.0f),
				new XYZ( 0.0f,  0.0f,  1.0f), new XYZ( 0.0f, 0.0f,  -1.0f),
				new XYZ( -1.0f,  0.0f,  0.0f), new XYZ(1.0f,  0.0f,  0.0f)};

		//	private XYZ[] Verticies = new XYZ[] {
		//			new XYZ(0.0f, 0.0f, 0.0f), new XYZ(0.0f, 0.0f, 1.0f),
		//			new XYZ(0.0f, 1.0f, 0.0f), new XYZ(0.0f, 1.0f, 1.0f),
		//			new XYZ(1.0f, 0.0f, 0.0f), new XYZ(1.0f, 0.0f, 1.0f),
		//			new XYZ(1.0f, 1.0f, 0.0f), new XYZ(1.0f, 1.0f, 1.0f)};            

		//	private XYZ[] Normals = new XYZ[] {
		//			new XYZ( 0.0f,  0.0f,  1.0f), new XYZ( 0.0f,  0.0f, -1.0f),
		//			new XYZ( 0.0f,  1.0f,  0.0f), new XYZ( 0.0f, -1.0f,  0.0f),
		//			new XYZ( 1.0f,  0.0f,  0.0f), new XYZ(-1.0f,  0.0f,  0.0f)};

		private int[] VerticiesMap = new int[] { 0, 6, 4, 0, 2, 6, 
				0, 3, 2, 0, 1, 3, 
				2, 7, 6, 2, 3, 7, 
				4, 6, 7, 4, 7, 5, 
				0, 4, 5, 0, 5, 1, 
				1, 5, 7, 1, 7, 3 };

		private int[] NormalsMap = new int[]   { 1, 1, 1, 1, 1, 1, 
				5, 5, 5, 5, 5, 5, 
				2, 2, 2, 2, 2, 2, 
				4, 4, 4, 4, 4, 4, 
				3, 3, 3, 3, 3, 3, 
				0, 0, 0, 0, 0, 0 };
		SensorConfigType type;
		@Override 
		public String toString() {
			if(type.equals(SensorConfigType.NEW))
				return "new";
			if(type.equals(SensorConfigType.BEST))
				return "best";
			if(type.equals(SensorConfigType.CURRENT))
				return "current";
			if(type.equals(SensorConfigType.INITIAL))
				return "initial";
			return "not set";
		}

		public DisplayTool(MultiDomainViewer.SensorConfigType type, Composite parent, int style, GLData data, ScenarioSet scenarioSet) {
			super(parent, style, data);
			this.type = type;
			this.scenarioSet = scenarioSet;
			if(scenarioSet == null  || 
					scenarioSet.getNodeStructure() == null ||
					scenarioSet.getNodeStructure().getX() == null || 
					scenarioSet.getNodeStructure().getX().isEmpty()) {

				List<Float> nodes = new ArrayList<Float>();
				for(float location = 0.0f; location < 20; location += 1.5) {
					nodes.add(location);
				}
				setDomainSize(nodes, nodes, nodes);			
			} else {
				setDomainSize(scenarioSet.getNodeStructure().getX(), scenarioSet.getNodeStructure().getY(), scenarioSet.getNodeStructure().getZ());	
			}

			setCurrent();
			try {
			context = GLDrawableFactory.getDesktopFactory().createExternalGLContext();
			
			}catch(Exception e) {
			}

			context.makeCurrent();
			/*
			 *      }

          //  Color bc = Color.FromArgb(0, 200, 200, 200);
            GL2.ClearColor(Color.White);    // Background Color -> transparent black

            GL2.ShadeModel(ShadingModel.Flat);
        //    GL2.Material(MaterialFace.Front, MaterialParameter.Specular, 1.0f);
       //     GL2.Material(MaterialFace.Front, MaterialParameter.Shininess, 50.0f);
            GL2.Light(LightName.Light0, LightParameter.Position, 1.0f);

            //  Enable 2D blending
            GL2.Enable(EnableCap.Blend);
            GL2.BlendFunc(BlendingFactorSrc.SrcAlpha, BlendingFactorDest.OneMinusSrcAlpha);

            //  Enable anti aliasing
           // GL2.Enable(EnableCap.LineSmooth);
           // GL2.Enable(EnableCap.PolygonSmooth);
           // GL2.CullFace(CullFaceMode.FrontAndBack);
            GL2.Enable(EnableCap.Lighting);
            GL2.Enable(EnableCap.Light0);
            GL2.DepthFunc(DepthFunction.Always);
           // GL2.DepthFunc(DepthFunction.Less);
            GL2.Enable(EnableCap.DepthTest);

			 * 
			 */

			GL2 gl = context.getGL().getGL2();

			gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
			gl.glClearDepth(1.0f);      // set clear depth value to farthest
			gl.glEnable(GL2.GL_DEPTH_TEST); // enables depth testing
			// 	gl.glDepthFunc(GL2.GL_ALWAYS);
			gl.glDepthFunc(GL2.GL_ALWAYS);  // the type of depth test to do
			gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST); // best perspective correction
			gl.glShadeModel(GL2.GL_SMOOTH); // blends colors nicely, and smoothes out lighting

			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LIGHTING);
			gl.glEnable(GL2.GL_LIGHT0);

			// Create light components.
			float ambientLight[] = { 0.4f, 0.4f, 0.4f, 1.0f };
			float diffuseLight[] = { 0.8f, 0.8f, 0.8f, 1.0f };
			float specularLight[] = { 0.5f, 0.5f, 0.5f, 1.0f };
			// From above float position[] = { offset.x-(distanceXYZ.x/2), offset.y-distanceXYZ.y/2, offset.z+distanceXYZ.z*2, 1.0f };
			// From +z  float position[] = {offset.x+(distanceXYZ.x), offset.y-distanceXYZ.y/2, offset.z-distanceXYZ.z/2, 1.0f}; // x

			float x = ((offset.x+distanceXYZ.x)+(offset.x-(distanceXYZ.x/2))+(offset.x-(distanceXYZ.x/2)));
			float y = ((offset.y+distanceXYZ.y)+(offset.y-distanceXYZ.y/2)+(offset.y-distanceXYZ.y/2));
			float z = ((offset.z+distanceXYZ.z*2)+(offset.z-distanceXYZ.z/2)+(offset.z-distanceXYZ.z/2));
			float position[] = {x, y, z, 1.0f};

			// Assign created components to GL_LIGHT0.
			gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight, 0);
			gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight, 0);
			gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight, 0);
			gl.glLightfv( GL2.GL_LIGHT0, GL2.GL_POSITION, position, 0);
			// 
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);	
			context.release();
			addListeners(this);

			/*
			(new Thread() {
				public void run() {
					try {
						while(!isDisposed()) {
							triggerDisplayThread();
						}
					} catch( Exception e ) {
						e.printStackTrace();
					}
				}
			}).start();
			 */

		}

		public void triggerDisplayThread() {
			try {
				getDisplay().syncExec(new Runnable() {
					public void run() {
						if (!isDisposed()) {
							updateDisplay();						
						}
					}
				});
			} catch (Exception e) { };
		}

		public void updateDisplay() {

			if(!isCurrent()) 
				setCurrent();

			context.makeCurrent();			

			GL2 gl = context.getGL().getGL2();

			gl.glViewport(0, 0, xBound, yBound);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();

			GLU glu = new GLU();
			glu.gluPerspective(zoom,  xBound / yBound, 0.5f, 1000000.0f);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();

			gl.glFlush();
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			//	gl.glClearColor(.9f, .9f, 1.0f, 1.0f);
			gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glClearDepth(1);
			gl.glLoadIdentity();

			glu.gluLookAt(0, lookat, 0,	0, 0, 0, 0, 0, -1);

			gl.glTranslatef(xtrans, 0f,  ytrans);
			gl.glRotatef(xrot, 0.0f, 0.0f, 1.0f); // z plane
			gl.glRotatef(yrot, 0.0f, 1.0f, 0.0f); // y plane
			//	gl.glColor3f(0.9f, 0.9f, 0.9f);

			DrawDomain();

			swapBuffers();

			context.release();

		}

		//	       6b---------7a
		//        /|         /|
		//		 / |        / |
		//      4d----------5c--3e
		//      | /2f      | /
		//		|/         |/	
		// 		0h----------1g
		public void drawNodes(GL2 gl) {

			/*
				Axis for debugging 
			//gl.glLineWidth(2.5f);
			float cyan[] = {0.f, .8f, .8f, 1.0f};
			float magenta[] = {.8f, 0.f, .8f, 1.0f};
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, magenta, 0);		
			gl.glBegin(GL2.GL_LINES);
			float x = ((offset.x+distanceXYZ.x)+(offset.x-(distanceXYZ.x/2))+(offset.x-(distanceXYZ.x/2)));
			float y = ((offset.y+distanceXYZ.y)+(offset.y-distanceXYZ.y/2)+(offset.y-distanceXYZ.y/2));
			float z = ((offset.z+distanceXYZ.z*2)+(offset.z-distanceXYZ.z/2)+(offset.z-distanceXYZ.z/2));
			System.out.println("XYZ:  " + x + ", " + y + ", " + z );

			gl.glVertex3f(x,y,z); // x
			gl.glVertex3f(x+1, y+1, z+1); // x
			gl.glEnd();

			float position[] = { offset.x, offset.y, offset.z*2, 1.0f };
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(position[0], position[1], position[2]); // x
			gl.glVertex3f(position[0], position[1], position[2]+1); // x
			gl.glEnd();

			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, cyan, 0);		
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(0.0f+offset.x, 0.0f+offset.y, 0.0f+offset.z); // x
			gl.glVertex3f(1.0f+offset.x, 0.0f+offset.y, 0.0f+offset.z); // x
			gl.glEnd();
			gl.glColor3f(0.0f, 255.0f, 0.0f);
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(0.0f+offset.x, 0.0f+offset.y, 0.0f+offset.z); // y			
			gl.glVertex3f(0.0f+offset.x, 1.0f+offset.y, 0.0f+offset.z); // y
			gl.glEnd();	
			gl.glColor3f(0.0f, 0.0f, 255.0f);		
			gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(0.0f+offset.x, 0.0f+offset.y, 0.0f+offset.z); // z			
			gl.glVertex3f(0.0f+offset.x, 0.0f+offset.y, 1.0f+offset.z); // z
			gl.glEnd();
			gl.glColor4f(255.0f, 255.0f, 255.0f, 180.0f);
			 */

			float[][] colors = new float[][]{
					{1.0f, 0.0f, 0.0f, 0.8f}, 
					{0.0f, 0.0f, 1.0f, 0.8f}, 
					{1.0f, 0.0f, 0.0f, 0.03f}, 
					{0.0f, 0.0f, 1.0f, 0.03f}, 
					{1.0f, 1.0f, 0.8f, 0.03f}};

			Map<String, List<Vector3f[]>> nodes = new HashMap<String, List<Vector3f[]>>();

			if(nodesByType != null)
				nodes.putAll(nodesByType);
			if(cloudsByType != null)
				nodes.putAll(cloudsByType);

			int numCubesDrawn = 0;
			gl.glBegin(GL2.GL_QUADS);  
			for(String type: nodes.keySet()) {
				List<Vector3f[]> cubes = nodes.get(type);
				//	System.out.println("Cubes of type: " + type + " = " + cubes.size());

				// Color this set of cubes, we need to know the type
				Color color = colorsByType.get(type);
				float[] colorF = {
						((float)color.getRed())/255.0f, 
						((float)color.getGreen())/255.0f, 
						((float)color.getBlue())/255.0f, 
						type.startsWith("cloud") ? 0.02f : 0.8f};
				gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, colorF, 0);
				//		System.out.println("Color: " + colorF[0] + ", " + colorF[1] + ", " + colorF[2] + ", " + colorF[3]);
				for(Vector3f[] cube : cubes) {
					if(cube.length == 8) {	// 3D
						int counter = 0; 
						for(int i: new int[]{	3, 2, 6, 7,	// Right, (+Y)
								5, 4, 0, 1, // Left, (-Y)
								7, 6, 4, 5,	// Top, (+Z)
								1, 0, 2, 3,	// Bottom, (-Z)
								6, 2, 0, 4, // Back, (-X)
								3, 7, 5, 1  // Front, (+X)
						}) {							
							if(counter % 4 == 0) {
								XYZ normal = Normals[counter/4];
								gl.glNormal3f(normal.x, normal.y, normal.z);
							}
							gl.glVertex3f(cube[i].x, cube[i].y, cube[i].z); // Top							
							counter++;
						}  
					}
					numCubesDrawn++;
				}
			}
			gl.glEnd();
			//	System.out.println("Num cubes: " + numCubesDrawn);
			float black[] = {0.0f, 0.0f, 0.0f, 1.0f};
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, black, 0);	

		}


		private void addListeners(DisplayTool tool) {

			tool.addListener(SWT.KeyDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					translating = true;
					//updateDisplay();
					triggerDisplayThread();

				}
			});

			tool.addKeyListener(new KeyAdapter()
			{	
				public void keyPressed(KeyEvent e){
					if(e.keyCode == 117) {
						uniform++;
						if (uniform == 3)
							uniform = 0;
					}
					//updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addKeyListener(new KeyAdapter()
			{	
				public void keyPressed(KeyEvent e){
					if(e.keyCode == 99) {
						cloud = !cloud;
					}
					//updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addKeyListener(new KeyAdapter()
			{	
				public void keyPressed(KeyEvent e){
					if(e.keyCode == 114) {
						resetCloud();
					}
					//updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addKeyListener(new KeyAdapter()
			{	
				public void keyPressed(KeyEvent e){
					if(e.keyCode == 109) {
						translating = !translating;
					}
					//updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addKeyListener(new KeyAdapter()
			{	
				public void keyPressed(KeyEvent e){
					if(e.keyCode == 116) {
						colorByType = !colorByType;
						resetCloud();
					}
					//updateDisplay();
					triggerDisplayThread();
				}
			});


			tool.addListener(SWT.KeyUp, new Listener() {
				@Override
				public void handleEvent(Event event) {
					translating = false;
					//updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addListener(SWT.MouseDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					pt = new Point(event.x, event.y);
					//	updateDisplay();
					triggerDisplayThread();
				}
			});

			tool.addListener(SWT.MouseMove, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if(pt == null)
						return;
					if(translating) {
						xtrans -= (pt.x-event.x)*10;
						ytrans += (pt.y-event.y)*10;
					} else {
						xrot -= (pt.x-event.x)*.25;
						yrot += (pt.y-event.y)*.25;
					}
					pt = new Point(event.x, event.y);
					triggerDisplayThread();
					//updateDisplay();
				}
			});

			tool.addListener(SWT.MouseUp, new Listener() {
				@Override
				public void handleEvent(Event event) {
					pt = null;
					triggerDisplayThread();
					//updateDisplay();
				}			
			});

			tool.addListener(SWT.MouseVerticalWheel, new Listener() {
				@Override
				public void handleEvent(Event event) {
					zoom-=event.count*.5;
					//updateDisplay();
					//	System.out.println(zoom);
					triggerDisplayThread();
				}
			});

			tool.addListener(SWT.Resize, new Listener() {
				public void handleEvent(Event event) {
					Rectangle bounds = getBounds();
					xBound = bounds.width;
					yBound = bounds.height;
					triggerDisplayThread();
					//updateDisplay();
				}
			});

		}



		/************************************************************
		 * Code copied from c# version
		 */


		/**
		 * Set size of domain here. 
		 */
		public void setDomainSize(List<Float> xs, List<Float> ys, List<Float> zs)
		{
			// These will be the centers, we actually want the vertices
			cellBoundsX = new ArrayList<Float>();
			for(int x = 1; x < xs.size(); x++) {
				float half = (xs.get(x)-xs.get(x-1))/2;
				if(x == 1)
					cellBoundsX.add(new Float(xs.get(x-1)-half).floatValue());
				cellBoundsX.add(new Float(xs.get(x-1)+half).floatValue());
				if(x == xs.size()-1) 
					cellBoundsX.add(new Float(xs.get(x)+half).floatValue());
			}

			cellBoundsY = new ArrayList<Float>();
			for(int y = 1; y < ys.size(); y++) {
				float half = (ys.get(y)-ys.get(y-1))/2;
				if(y == 1)
					cellBoundsY.add(new Float(ys.get(y-1)-half).floatValue());
				cellBoundsY.add(new Float(ys.get(y-1)+half).floatValue());
				if(y == ys.size()-1) 
					cellBoundsY.add(new Float(ys.get(y)+half).floatValue());
			}

			cellBoundsZ = new ArrayList<Float>();
			for(int z = 1; z < zs.size(); z++) {
				float half = (zs.get(z)-zs.get(z-1))/2;
				if(z == 1)
					cellBoundsZ.add(new Float(zs.get(z-1)-half).floatValue());
				cellBoundsZ.add(new Float(zs.get(z-1)+half).floatValue());
				if(z == zs.size()-1) 
					cellBoundsZ.add(new Float(zs.get(z)+half).floatValue());
			}

			nXYZ = new IJK(cellBoundsX.size() - 1, cellBoundsY.size() - 1, cellBoundsZ.size() - 1);

			// What about  negatives z=[-100, 10], need to use
			this.distanceXYZ = new XYZ(
					cellBoundsX.get(nXYZ.i) - cellBoundsX.get(0),
					cellBoundsY.get(nXYZ.j) - cellBoundsY.get(0),
					cellBoundsZ.get(nXYZ.k) - cellBoundsZ.get(0));

			translate.x = 0 -cellBoundsX.get(0);   // To move the points back to the center
			translate.y = 0 -cellBoundsY.get(0);
			translate.z = 0 -cellBoundsZ.get(0);

			// Amount of zoom at start
			rotate.z = (Math.max(Math.max(Math.abs(distanceXYZ.x), Math.abs(distanceXYZ.y)), Math.abs(distanceXYZ.z))); // Set this to the largest distance


			offset.x = distanceXYZ.x / 2; // Used to center the mesh
			offset.y = distanceXYZ.y / 2;
			offset.z = distanceXYZ.z / 2;

			// Used for uniform mesh
			deltaXYZ = new XYZ(
					distanceXYZ.x / (nXYZ.i),
					distanceXYZ.y / (nXYZ.j),
					distanceXYZ.z / (nXYZ.k));    // delta in x, y, z

			maxDelta = Math.max(Math.max(Math.abs(deltaXYZ.x), Math.abs(deltaXYZ.y)), Math.abs(deltaXYZ.z));

			minimum = new XYZ(-offset.x, -offset.y, -offset.z);
			maximum = new XYZ(
					cellBoundsX.get(cellBoundsX.size() - 1) -
					offset.x + translate.x, 
					cellBoundsY.get(cellBoundsY.size() - 1) - offset.y + translate.y, 
					cellBoundsZ.get(cellBoundsZ.size() - 1) - offset.z + translate.z);

			// Slightly different

			//  minXYZUniform
			maxXYZUniform = new XYZ((maxDelta * nXYZ.i) - offset.x, (maxDelta * nXYZ.j) - offset.y, (maxDelta * nXYZ.k) - offset.z);

			zoom  = 5425.2017f;
			lookat = (int) -(Math.max(Math.max(maxXYZUniform.x, maxXYZUniform.y), maxXYZUniform.z)*5);
			xrot = 90;
			yrot = -150;			

			resetCloud();
		}


		private void resetCloud()
		{
			// Fill the cloud
			cloudNodes = new HashMap<Integer, Integer>();
			sensorsMap = new HashMap<Integer, ExtendedSensor>();
			for (String type: scenarioSet.getDataTypes())   // May not have any
			{
				// Color all these cells transparent
				for (int nodeNumber: scenarioSet.getSensorSettings(type).getValidNodes(null))
				{
					if (!cloudNodes.containsKey(nodeNumber))
						cloudNodes.put(nodeNumber, 0);
					if (!sensorsMap.containsKey(nodeNumber))
						sensorsMap.put(nodeNumber, new ExtendedSensor(nodeNumber, type, scenarioSet.getNodeStructure()));
					int nodes = cloudNodes.get(nodeNumber) + 1;
					cloudNodes.put(nodeNumber, nodes);

				}
			}
			List<Integer> uniqueKeys = new ArrayList<Integer>();
			for(int key: cloudNodes.values()) {
				if (!uniqueKeys.contains(key))
					uniqueKeys.add(key);
			}

			int totalSensors = scenarioSet.getDataTypes().size();
			colors = new HashMap<Integer, Color>();
			for(int key: uniqueKeys) {
				float color = (float)key / (float)totalSensors;
				colors.put(key, new Color((int)(250f * (color)), (int)(250f * (color)), (int)(250f * (color))));
			}
			colorsByType = new HashMap<String, Color>();
			colorsByType.put("cloud_mix",  new Color(200, 50, 150)); // Mixed cloud, purple
			for(String type: scenarioSet.getDataTypes())   // May not have any
			{
				String temp = "Purple";
				if(type.contains("CO2") || type.contains("Saturation"))
					temp = "Red";
				else if(type.contains("Pressure"))
					temp = "Blue";
				else if(type.equalsIgnoreCase("ph")) 
					temp = "Orange";

				if(!colorsByType.containsKey(temp)) {
					Color tempColor = stringToColor(temp);
					colorsByType.put(type, new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), 255));
				}
				if(!colorsByType.containsKey("cloud_" + temp)) {
					Color tempColor = stringToColor(temp);
					colorsByType.put("cloud_" + type, new Color(tempColor.getRed(), tempColor.getGreen(), tempColor.getBlue(), 25));		
				}
			}
		}

		private Color stringToColor(String color)
		{
			Color returnColor;
			if (color.equals("Red"))
			{
				returnColor = Color.RED;
			}
			else if (color.equals("Green"))
			{
				returnColor = Color.GREEN;
			}
			else if (color.equals("Blue"))
			{
				returnColor =  Color.BLUE;
			}
			else if (color.equals("Orange"))
			{
				returnColor = Color.ORANGE;
			}
			else if (color.equals("Purple"))
			{
				returnColor = new Color(200, 50, 150);
			}
			else if (color.equals("Not Used"))
			{
				returnColor = Color.GRAY;
			}
			else
			{
				returnColor = new Color(20, 150, 150, 150);
			}
			return returnColor;
		}

		public void updateSensors(List<ExtendedSensor> sensors)
		{
			while (drawingLock) ;   // Wait for the lock to release
			this.sensors.clear();
			for(ExtendedSensor sensor : sensors) {
				this.sensors.add(sensor.makeCopy());
			}
			sensorChanged = true;   // Set the sensors changed flag, so we see the changes next timer tick.
		}

		float[] colorBlack  = {0.0f,0.0f,0.0f,1.0f};
		float[] colorWhite  = {1.0f,1.0f,1.0f,1.0f};
		float[] colorGray   = {0.6f,0.6f,0.6f,0.2f};
		float[] colorRed    = {1.0f,0.0f,0.0f,1.0f};
		float[] colorBlue   = {0.0f,0.0f,0.1f,1.0f};
		float[] colorYellow = {1.0f,1.0f,0.0f,1.0f};
		float[] colorLightYellow = {.5f,.5f,0.0f,1.0f};

		private synchronized void DrawDomain()
		{
			//	GL gl = context.getGL();
			//	FloatBuffer points = BufferUtil.newFloatBuffer( 4 );
			//	gl.glColor4f(0.5f, 0.5f, 0.9f, 1.0f);
			//	points.put(colorWhite);
			//	gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, points);
			switch (uniform)
			{
			case 0:
				DrawDomainNonUniform();
				break;
			case 1:
				DrawDomainUniform();
				break;
			case 2:
				DrawDomainUniformXYZ();
				break;
			}


		}

		/**
		 * This one will make sure x and y and z are all the same
		 * The other uniform make it uniform along each axis individually
		 */
		private void DrawDomainUniformXYZ()
		{

			int nX = showMesh ? cellBoundsX.size() - 1 : 0;
			int nY = showMesh ? cellBoundsY.size() - 1 : 0;
			int nZ = showMesh ? cellBoundsZ.size() - 1 : 0;

			//      GL2.Material(MaterialFace.FrontAndBack, MaterialParameter.Diffuse, Color.Linen);
			GL2 gl = context.getGL().getGL2();
			gl.glBegin(GL2.GL_LINES);  
			float black[] = {0.0f, 0.0f, 0.0f, 1.0f};
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, black, 0);	
			for (int intZ = 0; intZ <= nZ; intZ++)
			{
				float z = (maxDelta * intZ) -offset.z;
				for (int intX = 0; intX <= nXYZ.i; intX++)  // Draw lines in x direction
				{
					gl.glVertex3f((maxDelta * intX) - offset.x, -offset.y, z);
					gl.glVertex3f((maxDelta * intX) - offset.x, maxXYZUniform.y, z);

				}
				for (int intY = 0; intY <= nXYZ.j; intY++)   // Draw lines in y direction
				{
					gl.glVertex3f(-offset.x, (maxDelta * intY) - offset.y, z);
					gl.glVertex3f(maxXYZUniform.x, (maxDelta * intY) - offset.y, z);
				}
			}

			// X Direction
			for (int intX = 0; intX <= nXYZ.i; intX++)
			{
				for (int intY = 0; intY <= nY; intY++)  // Draw lines in z direction
				{
					gl.glVertex3f((maxDelta * intX) - offset.x, (maxDelta * intY) - offset.y, minimum.z);
					gl.glVertex3f((maxDelta * intX) - offset.x, (maxDelta * intY) - offset.y, maxXYZUniform.z);
				}
			}

			// Y Direction
			if (!showMesh)
			{
				for (int intZ = 0; intZ <= nXYZ.k; intZ++)
				{

					float z = (maxDelta * intZ) - offset.z;
					for (int intX = 0; intX <= nX; intX++)  // Draw lines in x direction
					{
						gl.glVertex3f((maxDelta * intX) - offset.x, -offset.y, z);
						gl.glVertex3f((maxDelta * intX) - offset.x, maxXYZUniform.y, z);
					}
					for (int intY = 0; intY <= nY; intY++)   // Draw lines in y direction
					{
						gl.glVertex3f(-offset.x, (maxDelta * intY) - offset.y, z);
						gl.glVertex3f(maxXYZUniform.x, (maxDelta * intY) - offset.y, z);
					}
				}
				for (int intX = 0; intX <= nX; intX++)
				{
					for (int intY = 0; intY <= nXYZ.j; intY++)  // Draw lines in z direction
					{
						gl.glVertex3f((maxDelta * intX) - offset.x, (maxDelta * intY) - offset.y, minimum.z);
						gl.glVertex3f((maxDelta * intX) - offset.x, (maxDelta * intY) - offset.y, maxXYZUniform.z);
					}
				}
			}
			gl.glEnd(); 
			DrawSensors();
			VisualizeCloud();
		}


		private void DrawDomainUniform()
		{
			int nX = showMesh ? cellBoundsX.size() - 1 : 0;
			int nY = showMesh ? cellBoundsY.size() - 1 : 0;
			int nZ = showMesh ? cellBoundsZ.size() - 1 : 0;

			GL2 gl = context.getGL().getGL2();
			gl.glBegin(GL2.GL_LINES); 
			float black[] = {0.0f, 0.0f, 0.0f, 1.0f};
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, black, 0);	
			for (int intZ = 0; intZ <= nZ; intZ++)
			{

				float z = (deltaXYZ.z * intZ) - offset.z;
				for (int intX = 0; intX <= nXYZ.i; intX++)  // Draw lines in x direction
				{
					gl.glVertex3f((deltaXYZ.x * intX) - offset.x, -offset.y, z);
					gl.glVertex3f((deltaXYZ.x * intX) - offset.x, offset.y, z);
				}
				for (int intY = 0; intY <= nXYZ.j; intY++)   // Draw lines in y direction
				{
					gl.glVertex3f(-offset.x, (deltaXYZ.y * intY) - offset.y, z);
					gl.glVertex3f(offset.x, (deltaXYZ.y * intY) - offset.y, z);
				}
			}

			// X Direction

			for (int intX = 0; intX <= nXYZ.i; intX++)
			{
				for (int intY = 0; intY <= nY; intY++)  // Draw lines in z direction
				{
					gl.glVertex3f((deltaXYZ.x * intX) - offset.x, (deltaXYZ.y * intY) - offset.y, minimum.z);
					gl.glVertex3f((deltaXYZ.x * intX) - offset.x, (deltaXYZ.y * intY) - offset.y, maximum.z);
				}
			}

			// Y Direction
			if (!showMesh)
			{
				for (int intZ = 0; intZ <= nXYZ.k; intZ++)
				{

					float z = (deltaXYZ.z * intZ) - offset.z;
					for (int intX = 0; intX <= nX; intX++)  // Draw lines in x direction
					{
						gl.glVertex3f((deltaXYZ.x * intX) - offset.x, -offset.y, z);
						gl.glVertex3f((deltaXYZ.x * intX) - offset.x, offset.y, z);
					}
					for (int intY = 0; intY <= nY; intY++)   // Draw lines in y direction
					{
						gl.glVertex3f(-offset.x, (deltaXYZ.y * intY) - offset.y, z);
						gl.glVertex3f(offset.x, (deltaXYZ.y * intY) - offset.y, z);
					}
				}
				for (int intX = 0; intX <= nX; intX++)
				{
					for (int intY = 0; intY <= nXYZ.j; intY++)  // Draw lines in z direction
					{
						gl.glVertex3f((deltaXYZ.x * intX) - offset.x, (deltaXYZ.y * intY) - offset.y, minimum.z);
						gl.glVertex3f((deltaXYZ.x * intX) - offset.x, (deltaXYZ.y * intY) - offset.y, maximum.z);
					}
				}
			}
			gl.glEnd();

			DrawSensors();
			VisualizeCloud();
		}

		private void DrawDomainNonUniform() {

			int nX = showMesh ? cellBoundsX.size() -1 : 0;
			int nY = showMesh ? cellBoundsY.size() -1 : 0;
			int nZ = showMesh ? cellBoundsZ.size() -1 : 0;

			GL2 gl = context.getGL().getGL2();
			gl.glBegin(GL2.GL_LINES); 
			float black[] = {0.0f, 0.0f, 0.0f, 1.0f};
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, black, 0);		
			for (int intZ = 0; intZ <= nZ; intZ++)  // From the bottom to the top (just the bottom if show mesh is false)
			{
				float z = (cellBoundsZ.get(intZ) - offset.z) + translate.z;
				for (int intX = 0; intX <= nXYZ.i; intX++)  // Draw lines in x direction
				{
					float x = (cellBoundsX.get(intX) - offset.x) + translate.x;
					gl.glVertex3f(x, minimum.y, z);
					gl.glVertex3f(x, maximum.y, z);
				}
				for (int intY = 0; intY <= nXYZ.j; intY++)   // Draw lines in y direction
				{
					float y = (cellBoundsY.get(intY) - offset.y) + translate.y;
					gl.glVertex3f(minimum.x, y, z);
					gl.glVertex3f(maximum.x, y, z);
				}
			}

			// X Direction
			for (int intX = 0; intX <= nXYZ.i; intX++) // For all X's
			{
				float x = (cellBoundsX.get(intX) - offset.x) + translate.x;
				for (int intY = 0; intY <= nY; intY++)  // Draw lines in z direction
				{
					float y = (cellBoundsY.get(intY) - offset.y) + translate.y;
					gl.glVertex3f(x, y, minimum.z);
					gl.glVertex3f(x, y, maximum.z);
				}
			}

			// Y Direction
			if (!showMesh)
			{
				for (int intZ = 0; intZ <= nXYZ.k; intZ++)
				{
					float z = (cellBoundsZ.get(intZ) - offset.z) + translate.z;
					for (int intX = 0; intX <= nX; intX++)  // Draw lines in x direction
					{
						float x = (cellBoundsX.get(intX) - offset.x) + translate.x;
						gl.glVertex3f(x, minimum.y, z);
						gl.glVertex3f(x, maximum.y, z);
					}
					for (int intY = 0; intY <= nY; intY++)   // Draw lines in y direction
					{
						float y = (cellBoundsY.get(intY) - offset.y) + translate.y;
						gl.glVertex3f(minimum.x, y, z);
						gl.glVertex3f(maximum.x, y, z);
					}
				}
				for (int intX = 0; intX <= nX; intX++)
				{
					float x = (cellBoundsX.get(intX) - offset.x) + translate.x;
					for (int intY = 0; intY <= nXYZ.j; intY++)  // Draw lines in z direction
					{
						float y = (cellBoundsY.get(intY) - offset.y) + translate.y;
						gl.glVertex3f(x, y, minimum.z);
						gl.glVertex3f(x, y, maximum.z);
					}
				}
			}
			gl.glEnd();

			DrawSensors();
			VisualizeCloud();
		}

		private void DrawSensors()
		{
			if(nodesByType == null) 
				nodesByType = new HashMap<String, List<Vector3f[]>>();

			// Make sure to clear these first
			for(String key: nodesByType.keySet()) {
				nodesByType.get(key).clear();
			}

			ExtendedConfiguration thisConfiguration = null;
			if(type.equals(SensorConfigType.NEW))
				thisConfiguration = newConfiguration;
			if(type.equals(SensorConfigType.BEST))
				thisConfiguration = bestConfiguration;
			if(type.equals(SensorConfigType.CURRENT))
				thisConfiguration = currentConfiguration;
			if(type.equals(SensorConfigType.INITIAL))
				thisConfiguration = initialConfiguration;

			if(thisConfiguration == null || thisConfiguration.getExtendedSensors() == null)
				return; // Don't draw the sensors yet

			for(ExtendedSensor sensor: thisConfiguration.getExtendedSensors())
			{
				if(!nodesByType.containsKey(sensor.getSensorType()))
					nodesByType.put(sensor.getSensorType(), new ArrayList<Vector3f[]>());
				drawCube(sensor, nodesByType.get(sensor.getSensorType()));		
			}
			GL2 gl = context.getGL().getGL2();
			drawNodes(gl); // Draw the nodes we just sent to the buffer
			gl.glEnd();
		}
		/*
		 * 
		 * public void drawNodes(GL gl) {
			for(Vector3f[] v : nodes) {
				if(v.length == 8) {	// 3D
					gl.glBegin(GL2.GL_QUADS);            // Draw A Quad

					for(int i: new int[]{	3, 2, 6, 7,	// Top
							5, 4, 0, 1, // Bottom
							7, 6, 4, 5,	// Front
							1, 0, 2, 3,	// Back
							6, 2, 0, 4, // Left
							3, 7, 5, 1  // Right
					}) {
						gl.glVertex3f(v[i].x, v[i].y, v[i].z); // Top
					}
					gl.glEnd();  
				}
			}

		//	       6b---------7a
		//        /|         /|
		//		 / |        / |
		//      4d----------5c--3e
		//      | /2f      | /
		//		|/         |/	
		// 		0h----------1g
		}
		 */
		public void drawCube(ExtendedSensor sensor, List<Vector3f[]> list)
		{		

			int i = sensor.getIJK().getI()-1;
			int j = sensor.getIJK().getJ()-1;
			int k = sensor.getIJK().getK()-1;

			Vector3f[] verticies = new Vector3f[8];
			for(int index = 0; index < 8; index++) {
				XYZ vertex = Vertices[index];
				switch (uniform)
				{
				case 0:
					float x = this.cellBoundsX.get(i + (int)vertex.x) - offset.x + translate.x;
					float y = this.cellBoundsY.get(j + (int)vertex.y) - offset.y + translate.y;
					float z = this.cellBoundsZ.get(k + (int)vertex.z) - offset.z + translate.z;
					verticies[index] = new Vector3f(x, y, z);
					break;
				case 1:
					verticies[index] = new Vector3f((deltaXYZ.x * (i + (int)vertex.x)) - offset.x, deltaXYZ.y * (j + (int)vertex.y) - offset.y, deltaXYZ.z * (k + (int)vertex.z) - offset.z);
					break;
				case 2:
					verticies[index] = new Vector3f((maxDelta * (i + (int)vertex.x)) - offset.x, maxDelta * (j + (int)vertex.y) - offset.y, maxDelta * (k + (int)vertex.z) - offset.z);
					break;
				}
			}

			list.add(verticies);
			/*
			GL gl = context.getGL();
			gl.glBegin(GL2.GL_TRIANGLES);
			for (int index = 0; index < VerticiesMap.length; index++)
			{
				XYZ vertex = Verticies[VerticiesMap[index]];
				XYZ normal = Normals[NormalsMap[index]];
				Vector3f tempVector = new Vector3f(); ;
				switch (uniform)
				{
				case 0:
					float x = this.cellBoundsX.get(i + (int)vertex.x) - offset.x + translate.x;
					float y = this.cellBoundsY.get(j + (int)vertex.y) - offset.y + translate.y;
					float z = this.cellBoundsZ.get(k + (int)vertex.z) - offset.z + translate.z;
					tempVector = new Vector3f(x, y, z);
					break;
				case 1:
					tempVector = new Vector3f((deltaXYZ.x * (i + (int)vertex.x)) - offset.x, deltaXYZ.y * (j + (int)vertex.y) - offset.y, deltaXYZ.z * (k + (int)vertex.z) - offset.z);
						break;
				case 2:
					tempVector = new Vector3f((maxDelta * (i + (int)vertex.x)) - offset.x, maxDelta * (j + (int)vertex.y) - offset.y, maxDelta * (k + (int)vertex.z) - offset.z);
					break;
				}

				//  if (normals)
				System.out.println("Drawing " + index + ": " + tempVector);
			//	gl.glNormal3f(normal.x, normal.y, normal.z);
				gl.glVertex3f(tempVector.x, tempVector.y, tempVector.z);


				//         if (triangles.Contains(tempVector) && drawingCloud)
				//           continue; // Don't need to redraw this one
				//    if(drawingCloud)
				//        triangles.Add(tempVector);
			}
			gl.glEnd();
			 */
		}

		private void VisualizeCloud() {
			if (!cloud)
				return; // because it is a bit slow, we can toggle it off

			if(cloudsByType == null)
				cloudsByType = new HashMap<String, List<Vector3f[]>>();

			for(List<Vector3f[]> cubes: cloudsByType.values())
				cubes.clear();

			// Fill the nodes array
			if(this.colorByType) {
				for(String type: scenarioSet.getDataTypes()) {
					for(int nodeNumber: scenarioSet.getSensorSettings(type).getValidNodes(null)) {
						ExtendedSensor cloudSensor = sensorsMap.get(nodeNumber);						
						boolean mix = false;
						for(String type2: scenarioSet.getDataTypes()) {
							if(type.equals(type2))
								continue;
							if(scenarioSet.getSensorSettings(type2).getValidNodes(null).contains(nodeNumber)) {
								mix = true;
							}
						}
						String key = "cloud_" + (mix ? "mix" : type);
						if(!cloudsByType.containsKey(key))
							cloudsByType.put(key, new ArrayList<Vector3f[]>());
						drawCube(cloudSensor, cloudsByType.get(key));					
					}
				}
			} else {
				if(cloudNodes.keySet().isEmpty()) {
					resetCloud();
				} else {
					if(!cloudsByType.containsKey("cloud_mix"))
						cloudsByType.put("cloud_mix", new ArrayList<Vector3f[]>());
					for(int nodeNumber: cloudNodes.keySet()) {
						drawCube(sensorsMap.get(nodeNumber), cloudsByType.get("cloud_mix"));
					}
				}
			} 

			GL2 gl = context.getGL().getGL2();
			drawNodes(gl); // Draw the nodes we just sent to the buffer
			gl.glEnd();
		}
	}
}
