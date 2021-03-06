package scrollthief.model;

/**
*
* @author Jon Pimentel
*/
public class Model {

	private OBJ obj;
	private int textureIndex;
	private Point3D location;
	double[] rotation;
	double scale;
	public double scaleX;
	private boolean flashRed = false;
	private int flashCount = 0;
	private boolean isTransparent;
	private boolean draw;
	private boolean texture;
	
	public Model(OBJ obj, int textureIndex, Point3D location, double[] rotation, double scale, double scaleX){
		this.obj= obj;
		this.textureIndex= textureIndex;
		this.location= location;
		this.rotation= rotation;
		this.scale= scale;
		this.scaleX= scaleX;
		
		isTransparent = false;
		this.draw = true;
	}
	
	public void setFlash(boolean flash) {
		flashRed = flash;
		if(flashRed){
			flashCount  = 5;
		}
	}
	
	public boolean isHurt(){
		if(flashCount == 0){
			flashRed = false;
		}
		if(flashRed && flashCount > 0){
			flashCount--;
		}
		return flashRed;
	}
	
	public void setAngle(double newAngle){
		double[] newRot= getRot();
		newRot[1]= newAngle;
		setRot(newRot);
	}
	
	// getters
	public OBJ getObj(){
		return obj;
	}
	
	public int getTxtr(){
		return textureIndex;
	}
	
	public Point3D getLoc(){
		return location;
	}
	
	public double[] getRot(){
		return rotation;
	}
	
	public double getAngle(){
		return rotation[1];
	}
	
	public double getScale(){
		return scale;
	}
	
	public boolean getIsTransparent() {
		return isTransparent;
	}
	
	// setters
	public void setLoc(Point3D newLoc){
		location= newLoc;
	}
	
	public void setRot(double[] newRot){
		rotation= newRot;
	}
	
	public void setScale(double newScale){
		scale= newScale;
	}
	
	public void setOBJ(OBJ newOBJ){
		obj= newOBJ;
	}
	
	public void setIsTransparent(boolean isTransparent) {
		this.isTransparent = isTransparent;
	}
	
	public void setShouldDraw(boolean value) {
		draw = value;
	}
	
	public boolean shouldDraw() {
		return draw;
	}
	
	public boolean hasTexture() {
		return textureIndex >= 0;
	}
}
