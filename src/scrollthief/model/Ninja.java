package scrollthief.model;

/**
 * @author Jon "Neo" Pimentel
 *
 * This class mostly just handles the animations of the Ninja, since they are not shared between characters 
 */
public class Ninja extends Character {
	
	OBJ[] standing;
	OBJ[] running;
	OBJ[] jumping;
	OBJ[] beginAttacking;
	OBJ[] attacking;
	boolean wasJumping= false;

	public Ninja(GameModel gameModel, Model model, double boxLength, double boxWidth) {
		super(gameModel, model, boxLength, boxWidth);
		standing= new OBJ[] {defaultOBJ};
		running= gameModel.getResource().getNinjaRun();
		attacking= running; //gameModel.getResource().getNinjaAttack();
		beginAttacking = new OBJ[] {attacking[0]}; 
		jumping= new OBJ[] {running[0]};
		motion= standing;
	}
	
	// Determine what OBJ to use this tick, and set it as model.obj
	public void animate(int tick){
		// If there is a change in behavior, switch to the appropriate motion
		if (isJumping && !wasJumping){ // Jumping animation takes priority (i.e. use regardless of speed)
			motion= jumping;
			animFrame= 0;
		}
		else if(isBeginAttacking) {
			motion= beginAttacking; //beginAttacking or attacking[0]?
			animFrame= 0;  //to be changed when we get animations
		}
		else if (isAttacking) {
			motion= attacking;  //attacking
			animFrame= 0;  //to be changed when we get animations
		}
		else if (speed == 0 && (oldSpeed != 0 || wasJumping) && !isJumping){ // Ninja is no longer moving
			motion= standing;
			animFrame= 0;
		}
		else if (speed != 0 && (oldSpeed == 0 || wasJumping) && !isJumping){ // Ninja has started running
			motion= running;
			animFrame= 0;
		}
		
		wasJumping= isJumping;
		oldSpeed= speed;
		
		// determine which frame of the loop to use, and use it
		if (motion.equals(running) || motion.equals(attacking)){
//			say("tick " + tick);
//			say("Running at speed " + speed);
//			say("2/speed = " + (int)(2/speed));
//			say("mod: " + tick % (int)(2/speed));
			if (tick % (int)(2/speed) == 0) 
				advanceFrame();
		}
		else{
			if (tick % 2 == 0)
				advanceFrame();
		}
		
		model.setOBJ(motion[animFrame]);
	}
	
	public void reset(){
		hp= 3;
		setLoc(new Point3D(0, 0, -5));
		setAngle(0);
		motion= standing;
		isJumping= false;
		setDeltaY(0);
		setGoalAngle(0);
		model.setOBJ(standing[0]);
		animFrame= 0;
		oldSpeed= 0;
	}

}
