package scrollthief.model.levels;

import java.util.ArrayList;
import java.util.Map;

import scrollthief.model.GameModel;
import scrollthief.model.Resource;

public class LevelFactory {
	private LevelState levelState;
	
	public LevelFactory() {
		levelState = LevelState.Level0;
	}
	
	public LevelFactory(LevelState state) {
		levelState = state;
	}
	
	public Level getNextLevel(Resource resource, GameModel gameModel, Map<String,ArrayList<String>> phrases) {
		if(levelState == LevelState.Level0)
			return new Level1(resource, gameModel, phrases);
//		if(levelState == LevelState.Level1)
//			return new Level2(resource, gameModel);
//		...
		
		return null;
	}
	
	public LevelState getLevelState() {
		return levelState;
	}
}
