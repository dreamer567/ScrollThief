package scrollthief.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class Obstacle {
	Model model;
	public boolean isLow;
	Point2D[][] hitBox;
	double boxWidth, boxLength, height;
	
	public Obstacle(Model model, boolean isLow, double boxLength, double boxWidth, double height){
		this.model= model;
		this.isLow= isLow;
		Point2D[] boxPoints= GameModel.findPoints(boxLength, boxWidth);
		hitBox= GameModel.boxToWorld(model, boxPoints);
		this.boxWidth= boxWidth;
		this.boxLength= boxLength;
		this.height= height;
	}
	
	public boolean isInBox(Point3D point){
		Point2D flatPoint= new Point2D.Double(point.z, point.x);
		Point2D objPoint= worldToObject(flatPoint);
		//say("flatPoint: "+flatPoint.toString()+", objPoint: "+objPoint.toString());
		
		if (objPoint.getX() > -boxLength && objPoint.getX() < boxLength
				&& objPoint.getY() > -boxWidth && objPoint.getY() < boxWidth)
			return true;
		
		return false;
	}
	
	public Point2D worldToObject(Point2D point){
		Point2D objPoint= new Point2D.Double();
		Point3D center= getLoc();
		double angle= model.getAngle();
		
		AffineTransform worldToObj= new AffineTransform();
		AffineTransform translate= new AffineTransform(1,0,0,1, -center.z, -center.x); 
		AffineTransform rotate= new AffineTransform(Math.cos(angle), -Math.sin(angle), 
				Math.sin(angle), Math.cos(angle), 0, 0);
		
		worldToObj.preConcatenate(translate);
		worldToObj.preConcatenate(rotate);
		worldToObj.transform(point, objPoint);
		
		//say("Transformed to point "+objPoint.toString());
		return objPoint;
	}
	
	public double getHeight(){
		return height;
	}
	
	public Point3D getLoc(){
		return model.getLoc();
	}
	
	public double[] getBoxDimensions(){
		double[] dim= {boxLength, boxWidth};
		return  dim;
	}
	
	@SuppressWarnings("unused")
	private void say(String message){
		System.out.println(message);
	}
}
