package scrollthief.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import scrollthief.model.Button;
import scrollthief.model.GameState;
import scrollthief.model.LoadingBar;
import scrollthief.model.Obstacle;
import scrollthief.model.Data;
import scrollthief.model.Point3D;
import scrollthief.model.Model;
import scrollthief.model.GameModel;
import scrollthief.model.characters.HitBox;

public class View extends GLCanvas implements GLEventListener{
	private static final long serialVersionUID = 1L;
	GameModel gameModel;
	GL2 gl;
	public boolean init= true;
	int windowX= 947;
	int windowY= 710;
	GLUT glut= new GLUT();
	float FOV= 45f;
	float ASPECT = 4f/3f;
	float NEAR= .1f;
	float FAR= 3000f;
	public boolean resetting= false;
	float[] lookFrom= {2,4,5};
	float[] lookAt= {0,2.5f,0};
	double cameraDistance= 6;
	double cameraAngle= 0;
	double cameraRotRate= 0;
	double[] cameraDelta= {0,0};
	DialogRenderer dialogRenderer;
	TextRenderer tRend;
	TextRenderer tRend2;
	TextRenderer tRendLoadingBar;
	TextRenderer tRendPause;
	boolean startDetectingObstacles;
	float heightTop= 15;
	float heightBottom= 1;
	Texture scrollTexture;

	public View(GameModel model){
		say("Loading view...");
		this.gameModel= model;
		setPreferredSize(new java.awt.Dimension(this.windowX, this.windowY));
        addGLEventListener(this);
        init= false;
		say("--View loaded--\n");
		startDetectingObstacles = false;
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
		gl= drawable.getGL().getGL2();
		dialogRenderer = new DialogRenderer(gl);
		gameModel.init(gl);
		tRend= new TextRenderer(new Font("Helvetica", Font.BOLD, 30));
		tRend2= new TextRenderer(new Font("Helvetica", Font.BOLD, 60));
		tRendLoadingBar= new TextRenderer(new Font("Helvetica", Font.PLAIN, 10));
		tRendPause = new TextRenderer(new Font("Helvetica", Font.BOLD, 15));
		loadScrollTexture(gl);
		
		setupLighting(gl);
		
		gl.glShadeModel(GL2.GL_SMOOTH);							// Enable Smooth Shading
		gl.glClearColor(0.5f, 0.5f, 0.5f, 0.5f);				// grey Background
		gl.glClearDepth(1.0f);									// Depth Buffer Setup
		gl.glEnable(GL2.GL_DEPTH_TEST);							// Enables Depth Testing
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glDepthFunc(GL2.GL_LEQUAL);								// The Type Of Depth Testing To Do
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		GLU glu= new GLU();
		glu.gluPerspective(FOV, ASPECT, NEAR, FAR);
		glu.gluLookAt(lookFrom[0], lookFrom[1], lookFrom[2],
		        lookAt[0], lookAt[1], lookAt[2],
		        0.0f, 1.0f, 0.0f);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		if (init){
			return;
		}
		gl = drawable.getGL().getGL2();		
		
		// apply world-to-camera transform
		world2camera(gl);
				
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glColor3f(1f, 1f, 1f);
		

		if(gameModel.getState() != GameState.ResourceLoading && gameModel.getState() != GameState.LevelLoading 
				&& gameModel.getState() != GameState.Start){

			ArrayList<Model> models= gameModel.getModels();
			Texture[] textures= gameModel.getResource().getTextures();
			// draw each model with its texture
			for (int i= 0; i < models.size(); i++){
				Model model= models.get(i);
				if (!model.shouldDraw())
					continue;
				gl.glPushMatrix();
				
				// don't draw guards if they are obscured
				if (model.getTxtr() == 8 && cullObscured(model)) continue;
				
				// assign texture
				if (model.hasTexture()){
					gl.glEnable(GL2.GL_TEXTURE_2D);
					textures[model.getTxtr()].bind(gl);
				}
				else{
					gl.glColor3f(1,1,1);
					gl.glDisable(GL2.GL_TEXTURE_2D);
				}
					
				if(model.isHurt()){
					gl.glColor3f(1,0,0);
				}
				else {
					gl.glColor3f(1,1,1);
				}
				
				//apply object to window transform
				obj2world(gl, model);
				
				model.getObj().DrawModel(gl, model.getIsTransparent());
				
				gl.glPopMatrix();
				gl.glFlush();
			}
			
			// draw player health
			if(gameModel.getNinja() != null && gameModel.getHeart() != null) {
				drawPlayerHealth(gl, gameModel.getHeart());
			}
			
			// draw boss health if close enough
			if(gameModel.getBoss() != null && gameModel.getBoss().isNear()) {
				drawBossHealth(gl);
			}
		}
		else if(gameModel.getState() == GameState.ResourceLoading){
			
			//Display the 
			displaySplashImage(gameModel.getSplashImage());
			
			//Display Loading Bar
			LoadingBar loading = gameModel.getResourceLoadingBar();

			drawLoadingBar(gl,loading);
			
			//Display Game Title
//			String text = "THE SCROLL THIEF";
//			dialogRenderer.overlayText(text, (int)(Data.windowX/2 - Data.windowX*.15), Data.windowY-100, Color.white, "reg");
			
			gl.glEnable(GL2.GL_TEXTURE_2D);
		}
		else if(gameModel.getState() == GameState.LevelLoading || gameModel.getState() == GameState.Start){
			//Display the splash
			displaySplashImage(gameModel.getLevelSplashImage());
			
			//Display Loading Bar
			LoadingBar loading = gameModel.getLevelLoadingBar();
			drawLoadingBar(gl,loading);
			
			if(gameModel.getState() == GameState.Start){

				String text = "Press Start to Begin Mission";
				dialogRenderer.overlayText(text, (int)(Data.windowX/2 - Data.windowX*.15), Data.windowY-100, Color.white, "reg");
			}
			
			gl.glEnable(GL2.GL_TEXTURE_2D);
		}
		if (gameModel.getState() == GameState.Paused){
//			String text= "Steal the enemy battle plans (scroll)";
//			dialogRenderer.overlayText(text,  Data.windowX/2 - (15 * text.length()/2), Data.windowY/2 + 150, Color.blue, "reg");
//			text= "without being detected";
//			dialogRenderer.overlayText(text,  Data.windowX/2 - (15 * text.length()/2), Data.windowY/2 + 100, Color.blue, "reg");
//			text= "Press start to begin";
//			dialogRenderer.overlayText(text,  Data.windowX/2 - (30 * text.length()/2), Data.windowY/2, Color.blue, "big");

			drawPauseMenu(gl);
//			String text= "Steal the enemy battle plans (scroll)";
//			overlayText(text,  windowX/2 - (15 * text.length()/2), windowY/2 + 150, Color.blue, "reg");
//			text= "without being detected";
//			overlayText(text,  windowX/2 - (15 * text.length()/2), windowY/2 + 100, Color.blue, "reg");
//			text= "Press start to continue";
//			overlayText(text,  windowX/2 - (30 * text.length()/2), windowY/2, Color.blue, "big");
		}
		else if (gameModel.getState() == GameState.Spotted){
			String text= "You've been spotted!";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (30 * text.length()/2), Data.windowY/2 + 100, Color.blue, "big");
			
			text= "Mission Failed! Press start to try again...";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (15 * text.length()/2), Data.windowY/2 + 50, Color.blue, "reg");
		}
		else if (gameModel.getState() == GameState.Killed){
			String text= "You've been killed!";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (30 * text.length()/2), Data.windowY/2 + 100, Color.red, "big");
			
			text= "Mission Failed! Press start to try again...";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (15 * text.length()/2), Data.windowY/2 + 50, Color.blue, "reg");
		}
		else if (gameModel.getState() == GameState.Victory){
			String text= "Mission Complete!";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (30 * text.length()/2), Data.windowY/2 + 100, Color.blue, "big");
			
			text= "You obtained the enemy battle plans!";
			dialogRenderer.overlayText(text,  Data.windowX/2 - (15 * text.length()/2), Data.windowY/2 + 50, Color.blue, "reg");
		}
		else if(gameModel.getState() == GameState.Dialog) {
			if(getDialogRenderer().isFinished()) {
				gameModel.changeState(GameState.Playing);
				gameModel.getCurrentLevel().getCurrentDialogHotspot().setRepeatable(false);
			}
			else
				dialogRenderer.render(gameModel.getCurrentLevel().getCurrentDialogHotspot().getText());
		}
		else if(gameModel.getState() == GameState.MainMenu){
			displaySplashImage(gameModel.getSplashImage());

			//Display Game Title
			//String text = "THE SCROLL THIEF";
			//overlayText(text, (int)(Data.windowX/2 - Data.windowX*.15), Data.windowY-100, Color.white, "reg");
			
			
			drawMainMenu(gl);
		}
	}
	

	private void drawMainMenu(GL2 gl){
		displaySplashImage(scrollTexture);
		gl.glDisable(GL2.GL_TEXTURE_2D);
				
		String text = "MAIN MENU";
		drawButtonMenu(gl,gameModel.getMainMenuButtons(), text);
		
	    gl.glFlush();

		gl.glPopMatrix();
		gl.glEnable(GL2.GL_TEXTURE_2D);
		
	}
	
	private void drawPauseMenu(GL2 gl){
		displaySplashImage(scrollTexture);
		gl.glDisable(GL2.GL_TEXTURE_2D);

		String text = "GAME PAUSED";
		
		drawButtonMenu(gl, gameModel.getPauseButtons(), text);

	    gl.glFlush();

		gl.glPopMatrix();
		gl.glEnable(GL2.GL_TEXTURE_2D);
		
	}
	
	private void drawButtonMenu(GL2 gl, List<Button> buttons, String text){
		double height = 300;
		double maxWidth = 300;
		double leftX = (Data.windowX - maxWidth)/2;
		double leftY = (Data.windowY - height)/2;
		overlayText(text, (int)(leftX + 100), (int)(leftY + height - 20), Color.black, "pause");
		for(Button b : buttons){
			b.setOffset((int)leftX, (int)leftY);
		}
		for(Button b : buttons){
			if(b.IsSelected()){
				gl.glColor3f(1f, 1f, 1f);
				gl.glBegin(GL2.GL_QUADS);
					gl.glVertex2d(b.getX()-2, b.getY()-2);
					gl.glVertex2d(b.getX()-2, b.getY()+b.getHeight()+2);
					gl.glVertex2d(b.getX()+b.getWidth()+2, b.getY()+b.getHeight()+2);
					gl.glVertex2d(b.getX()+b.getWidth()+2, b.getY()-2);
				gl.glEnd();
			}
			gl.glColor3f(1f, 1f, 0f);
			gl.glBegin(GL2.GL_QUADS);
				gl.glVertex2d(b.getX(), b.getY());
				gl.glVertex2d(b.getX(), b.getY()+b.getHeight());
				gl.glVertex2d(b.getX()+b.getWidth(), b.getY()+b.getHeight());
				gl.glVertex2d(b.getX()+b.getWidth(), b.getY());
			gl.glEnd();
			text = b.getText();
			int x = (int)(b.getX()+b.getWidth()/5);
			if(text.length() > 7){
				x -= 5*(text.length()-7);
			}
			int y = (int)(b.getY() + b.getHeight()/3);
			overlayText(b.getText(),x ,y , Color.black, "pause");
		}
	}
	
	private void displaySplashImage(Texture t){
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, Data.windowX, Data.windowY, 0, -10, 10);
		gl.glEnable( GL2.GL_TEXTURE_2D );
		gl.glBlendFunc (GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		t.bind(gl);
		gl.glLoadIdentity();
		gl.glColor3f(1, 1, 1);
		gl.glBegin( GL2.GL_QUADS );
			double originX = -1.0;//-(windowX/2);
			double originY = -1.0;//-(windowY/2);
			
			double x = 1.0;//windowX;
			double y = 1.0;//windowY;
			gl.glTexCoord2d(0.0,0.0); gl.glVertex2d(originX,originY);
			gl.glTexCoord2d(1.0,0.0); gl.glVertex2d(x,originY);
			gl.glTexCoord2d(1.0,1.0); gl.glVertex2d(x,y);
			gl.glTexCoord2d(0.0,1.0); gl.glVertex2d(originX,y);
		gl.glEnd();
		
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glPopMatrix();
	}
	
	private void drawPlayerHealth(GL2 gl, Texture t) {
		int ninjaHealth = gameModel.getNinja().getHP();
		double offset = 0;
		
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, Data.windowX, Data.windowY, 0, -10, 10);
		//gl.glEnable( GL2.GL_TEXTURE_2D );
		
		gl.glEnable (GL2.GL_BLEND);
		gl.glBlendFunc (GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
		for(int i = 0; i < ninjaHealth; i++) {
			// Logic for drawing each individual heart
			t.bind(gl);
			gl.glLoadIdentity();
			gl.glBegin( GL2.GL_QUADS );
				double originX = -0.85 + offset;
				double originY = 0.85;
				double x = -0.95 + offset;
				double y = 0.95;
				
				//these values define what part of the image renders on the screen from 0.0 to 1.0
				double coordOriginX = 0.0;
				double coordOriginY = 0.0;
				double coordX = 1.0;
				double coordY = 1.0;
				
				gl.glTexCoord2d(coordOriginX, coordOriginY); gl.glVertex2d(originX,originY);
				gl.glTexCoord2d(coordX, coordOriginY); gl.glVertex2d(x,originY);
				gl.glTexCoord2d(coordX, coordY); gl.glVertex2d(x,y);
				gl.glTexCoord2d(coordOriginX, coordY); gl.glVertex2d(originX,y);
			gl.glEnd();
			
			
			gl.glPopMatrix();
			gl.glFlush();
			offset += 0.125;
		}
		
		gl.glDisable(GL2.GL_BLEND);
		//gl.glDisable( GL2.GL_TEXTURE_2D );
	}
	
	private void drawBossHealth(GL2 gl) {
		String text = "BOSS";
		dialogRenderer.overlayText(text, Data.windowX - 300, Data.windowY - 44, Color.red, "reg");
		
		// Logic for drawing the boss health
		double leftX = Data.windowX - 230;
		double leftY = Data.windowY - 685;
		float percentage = ((float)gameModel.getBoss().getHP()/(float)gameModel.getBoss().getFullHP());
		float width = 200 * percentage;
		int maxWidth = 200;
		int height = 25;
		gl.glDisable( GL2.GL_TEXTURE_2D );
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, Data.windowX, Data.windowY, 0, -10, 10);
		gl.glColor3f(((float)204/(float)255), 0f, 0f);
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2d(leftX, leftY);
			gl.glVertex2d(leftX, leftY+height);
			gl.glVertex2d(leftX+maxWidth, leftY+height);
			gl.glVertex2d(leftX+maxWidth, leftY);
		gl.glEnd();

		gl.glColor3f(1f, 1f, ((float)51/(float)255));
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2d(leftX, leftY);
			gl.glVertex2d(leftX, leftY+height);
			gl.glVertex2d(leftX+width, leftY+height);
			gl.glVertex2d(leftX+width, leftY);
		gl.glEnd();
	    gl.glFlush();

		gl.glPopMatrix();
		gl.glEnable( GL2.GL_TEXTURE_2D );
	}
	
	private void drawLoadingBar(GL2 gl, LoadingBar loading){
		double leftX = Data.windowX/2 - Data.windowX*.05;
		double leftY = Data.windowY - 100;
		float percentage = ((float)loading.getProgress()/(float)loading.getTotal());
		float width = 100 * percentage;
		int maxWidth = 100;
		int height = 25;
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0, Data.windowX, Data.windowY, 0, -10, 10);
		gl.glColor3f(((float)204/(float)255), 0f, 0f);
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2d(leftX, leftY);
			gl.glVertex2d(leftX, leftY+height);
			gl.glVertex2d(leftX+maxWidth, leftY+height);
			gl.glVertex2d(leftX+maxWidth, leftY);
		gl.glEnd();

		gl.glColor3f(1f, 1f, ((float)51/(float)255));
		gl.glBegin(GL2.GL_QUADS);
			gl.glVertex2d(leftX, leftY);
			gl.glVertex2d(leftX, leftY+height);
			gl.glVertex2d(leftX+width, leftY+height);
			gl.glVertex2d(leftX+width, leftY);
		gl.glEnd();
	    gl.glFlush();

		gl.glPopMatrix();
		
		String text = loading.getLoadingText();
		dialogRenderer.overlayText(text, (int)(leftX), (int)Data.windowY/12, Color.white, "loading");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		Data.windowX= width;
		Data.windowY= height;
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(x, y, width, height);
		
		ASPECT = (float)width / (float)height;
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		GLU glu= new GLU();
		glu.gluPerspective(FOV, ASPECT, NEAR, FAR);
		glu.gluLookAt(lookFrom[0], lookFrom[1], lookFrom[2],
		        lookAt[0], lookAt[1], lookAt[2],
		        0.0f, 1.0f, 0.0f);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	@Override
	public void dispose(GLAutoDrawable drawable) {
		
	}
	
	public void overlayText(String text, int x, int y, Color color, String type){
		TextRenderer rend= tRend;
		if (type == "big")
			rend= tRend2;
		else if(type == "loading")
			rend = tRendLoadingBar;
		else if(type == "pause")
			rend = tRendPause;
			
		rend.setColor(color);
		rend.beginRendering(Data.windowX, Data.windowY, true);
		rend.draw(text, x, y);
		rend.endRendering();
		rend.flush();
	}
	
	private void setupLighting(GL2 gl){
		float[] lightPos = { 2000,3000,2000,1 };        // light position
		float[] ambiance = { 0.4f, 0.4f, 0.4f, 1f };     // low ambient light
		float[] diffuse = { 1f, 1f, 1f, 1f };        // full diffuse color

		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_LIGHT0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambiance, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuse, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION,lightPos, 0);
	}
	
	// calculates and applies the transformation matrix for object to world
	private void obj2world(GL2 gl, Model model){
		double[] rot= model.getRot();
		double angle= Math.toDegrees(rot[1]);
		double angleX= Math.toDegrees(rot[0]);
		double scale= model.getScale();
		double scaleX= scale * model.scaleX;
		Point3D loc= model.getLoc();
	
		gl.glTranslated(loc.x, loc.y, loc.z);
		gl.glRotated(angle, 0, -1, 0);
		gl.glRotated(angleX, 1, 0, 0);
		gl.glScaled(scaleX, scale, scale);
	}
	
	private void world2camera(GL2 gl){
		float scale= .6f; 
		double dZ= 0;
		double dX= 0;
		double ninjaAngle = gameModel.getNinjaAngle();
		Point3D ninjaLoc= gameModel.getNinjaLoc();
		lookAt= new float[]{(float) ninjaLoc.x, (float) ninjaLoc.y * scale + 2, (float) ninjaLoc.z};
		
		if (resetting){ // center the camera behind the ninja
			cameraAngle= ninjaAngle;
			cameraDistance= 6;
			lookFrom[1]= 4;
			resetting = false;
		}
		
		dZ= -Math.cos(cameraAngle); 
		dX= Math.sin(cameraAngle);
		
		lookFrom[2]= lookAt[2] + (float) (cameraDistance * dZ);
		lookFrom[0]= lookAt[0] + (float) (cameraDistance * dX);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		GLU glu= new GLU();
		glu.gluPerspective(FOV, ASPECT, NEAR, FAR);
		glu.gluLookAt(lookFrom[0], lookFrom[1], lookFrom[2],
		        lookAt[0], lookAt[1], lookAt[2],
		        0.0f, 1.0f, 0.0f);
		
		Point3D cameraLoc = new Point3D(lookFrom[0], lookFrom[1], lookFrom[2]);
		ninjaLoc = gameModel.getNinjaLoc();
		
		if(gameModel.getState() == GameState.Playing) {
			startDetectingObstacles = true;
		}
	
		// detect when an obstacle is between the viewer and the ninja and give it transparency
		if(ninjaLoc != null && startDetectingObstacles) {
			Line2D cameraToNinja = new Line2D.Double(cameraLoc.to2D(), ninjaLoc.to2D());
			for(Obstacle obstacle : gameModel.getObstacles()) {
				if(obstacle != null) {
					if(!HitBox.getCollidedEdges(cameraToNinja, obstacle.getHitBox()).isEmpty() && obstacle.getModel().getObj() != gameModel.getResource().getOBJs()[4]) {
						obstacle.getModel().setIsTransparent(true);
					}
					else {
						obstacle.getModel().setIsTransparent(false);
					}
				}
			}
		}

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		//gl.glLoadIdentity();
	}
	
	public DialogRenderer getDialogRenderer() {
		return dialogRenderer;
	}
	
	public void loadScrollTexture(GL2 gl) {
		GLProfile profile= gl.getGLProfile();
		try{
			BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream(Data.SCROLL_FILE));
			ImageUtil.flipImageVertically(image);
			scrollTexture = AWTTextureIO.newTexture(profile, image, false);
		}catch(IOException e){
			System.out.println("Error Loading scroll Image");
		}
 	}
	
	private boolean cullObscured(Model model){
		Point3D modelLoc= model.getLoc();
		Point3D cameraLoc= new Point3D((double)lookFrom[0], (double)lookFrom[1], (double)lookFrom[2]);
		Obstacle[] obstacles= gameModel.getObstacles();
		
		for (int i= 0; i < obstacles.length; i++){
			Obstacle obs= obstacles[i];
			int type = obs.getModel().getTxtr();
			
			// don't count pillars or tables, or transparent obstacles
			if (type == 3 || type == 4 || obs.getModel().getIsTransparent()) continue; 
			
			// see if the line between camera and model crosses obstacle hitbox	
			Line2D cameraToNinja = new Line2D.Double(modelLoc.to2D(), cameraLoc.to2D());
			if (!HitBox.getCollidedEdges(cameraToNinja, obs.getHitBox()).isEmpty()) 
				return true;
		}
		
		return false;
	}
	
// -----------------Camera getters -----------------------------
	public double getCamDistance(){
		return cameraDistance;
	}
	
	public float getCamHeight(){
		return lookFrom[1];
	}
	
	public double getCamAngle(){
		return cameraAngle;
	}
	
	public double[] getCamDelta(){
		return cameraDelta;
	}
	
// -----------------Camera setters ------------------------------------
	public void setCamDistance(double distance){
		cameraDistance= distance;
	}
	
	public void setCamHeight(float height){
		if(height < heightTop && height > heightBottom)
			lookFrom[1]= height;
	}
	
	public void setCamAngle(double angle){
		cameraAngle= angle;
	}
	
	public void setCamRotRate(double rate){
		cameraRotRate= rate;
	}
	
	public void setCamDelta(double[] delta){
		cameraDelta= delta;
	}
// --------------------------------------------------------------------
	
	private void say(String message){
		System.out.println(message);
	}
}
