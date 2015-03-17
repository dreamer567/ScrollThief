package scrollthief.model;

import java.util.ArrayList;
import java.util.Random;

import scrollthief.model.Point3D;

public class Boss extends Character{
	ArrayList<Character> projectiles= new ArrayList<Character>();
	double sightRange= 17;
	int tickCount= 0;
	OBJ[] standing;
	OBJ[] pouncing;
	boolean inBattle= false;
	Random randomGenerator;
	int difficultLevel = 5; //between 0 and 5 (5 is hardest)
	Point3D lastPouncePoint;
	boolean readyForAttack;
	int cooldown = 0;
//	enum State {STANDING, POUNCING, READY_FOR_ATTACK, SMALLATTACK, BIGATTACK, COOLDOWN};
//	State state;
	final int ATTACK_SIZE_SMALL = 1;
	final int ATTACK_SIZE_BIG = 2;
	final int TICKS_BETWEEN_POUNCES = 1000;
	final int TICKS_BETWEEN_ATTACKS = 75;
	final int TICKS_FOR_COOLDOWN = 200;
	final int PROBABILITY_OF_BIG_ATTACK = 30; //out of 100
	final int PROBABILITY_OF_SMALL_ATTACK = 60; //out of 100
	
	public Boss(GameModel gameModel, Model model, double boxLength, double boxWidth) {
		super(gameModel, model, boxLength, boxWidth);
		lastPouncePoint = gameModel.getCurrentLevel().getBossPouncePoints().get(0);
		turnRate= .02;
		setSpeed(.2);
		standing= new OBJ[] {model.getObj()};
		pouncing= gameModel.getResource().getBossPounce();
		motion= standing;
		hp=100;
		randomGenerator = new Random();
//		state = State.STANDING;
	}
	
	public void update(){
		if (!isNear() || !alive)
			return;
		
		tickCount++;
		if(tickCount > TICKS_BETWEEN_POUNCES) {
			pounce();
		}
		else {
			navigate();
			handleAttack();
			move();
		}
	}
	
	public void animate(int tick){		
		if (isNear() && !inBattle){
			inBattle= true;
			motion= standing;
			animFrame= 0;
		}
		
		if (tick % 2 == 0)
			advanceFrame();
		
		model.setOBJ(motion[animFrame]);
	}
	
	public void reset(){
		setAngle(0);
		setGoalAngle(0);
		setLoc(new Point3D(0,0,76));
	}
	
	private void navigate(){
		Point3D ninjaLoc= gameModel.getNinjaLoc();
		faceToward(ninjaLoc);
	}
	
	private void pounce() {
		tickCount= 0;
		if(gameModel.getCurrentLevel().getBossPouncePoints().size() <= 1)
			return;
		setNextPouncePoint();
		
		Data.say("pouncing to " + lastPouncePoint);
	}
	
	private void handleAttack() {
		if(cooldown > 0) {
			cooldown--;
			return;
		}
		if(tickCount % TICKS_BETWEEN_ATTACKS == 0)
			readyForAttack = true;
		if(!isFacingNinja() || !readyForAttack)
			return;
		
		int rand = getRand(TICKS_BETWEEN_POUNCES);
		
		if((rand -= PROBABILITY_OF_BIG_ATTACK) < 0) {
			shoot(ATTACK_SIZE_BIG);
		}
		else if((rand -= PROBABILITY_OF_SMALL_ATTACK) < 0) {
			shoot(ATTACK_SIZE_SMALL);
		}
	}
	
	private void setNextPouncePoint() {
		int rand = getRand(gameModel.getCurrentLevel().getBossPouncePoints().size());
		while(gameModel.getCurrentLevel().getBossPouncePoints().get(rand) == lastPouncePoint)
			rand = getRand(gameModel.getCurrentLevel().getBossPouncePoints().size());
		lastPouncePoint = gameModel.getCurrentLevel().getBossPouncePoints().get(rand);
	}
	
	private int getRand(int max) {
		return randomGenerator.nextInt(max);
	}
	
	//attackSize: 1 = small, 2 = large
	private void shoot(int attackSize) {
		if(attackSize == ATTACK_SIZE_BIG) {
			cooldown = TICKS_FOR_COOLDOWN;
		}
		readyForAttack = false;
		Data.say("Boss Attacking with power of " + attackSize);
		double scale= 3;
		double direction= getAngle() - Math.PI;
		OBJ[] objs= gameModel.getResource().getOBJs();
		Point3D ninjaLoc= gameModel.getNinjaLoc();
		// calculate target vector
		Point3D bossHead= new Point3D(getLoc().x, getLoc().y + 3.2, getLoc().z);
		double dist= ninjaLoc.minus(bossHead).length();
		
		double targetX= Math.sin(direction);
		double targetY= (-1/dist) * scale;
		double targetZ= -Math.cos(direction);
		
		Point3D targetVector= new Point3D(targetX, targetY, targetZ);
		
		// create projectile
		scale= 2;
		double[] rot = model.getRot().clone();
		rot[0]= -targetY * scale;
		Model projModel= new Model(objs[8], 11, bossHead, rot, .4, 1);
		gameModel.getProjectiles().add(new Projectile(gameModel, projModel, targetVector));
		gameModel.getModels().add(projModel);
	}

	public Point3D calcDelta(double deltaX, double deltaZ){
		Point3D scrollLoc= gameModel.getScroll().getLoc();
		
		Point3D input= new Point3D(deltaX, 0, deltaZ);
		Point3D scrollNormal= getLoc().minus(scrollLoc);
		scrollNormal.Normalize();
		
		Point3D undesired= scrollNormal.mult(input.dot(scrollNormal));
		Point3D desired= input.minus(undesired); 
		
		return desired;
	}
	
	public boolean isNear(){
		Point3D loc= model.getLoc();
		Point3D ninjaLoc= gameModel.getNinjaLoc();
		
		double distance= ninjaLoc.minus(loc).length();
		
		if (distance < sightRange)
			return true;
		
		return false;
	}
	
	private boolean isFacingNinja(){
		float fov= .75f; // (radians) needs tuning
		Point3D loc= getLoc();
		Point3D ninjaLoc= gameModel.getNinjaLoc();
		double bossAngle= -getAngle();
		double angToNinja= Math.atan2(ninjaLoc.x - loc.x, ninjaLoc.z - loc.z);
		
		// find the smallest difference between the angles
		double angDif = (angToNinja - bossAngle);
		angDif = gameModel.floorMod((angDif + Math.PI),(2 * Math.PI)) - Math.PI; 
		//say("Angle difference is: "+angDif);
		
		// check if this angle difference is small enough that the ninja would be in boss's FOV
		if (angDif > -fov && angDif < fov)
			return true;
		
		return false;
	}
}
