package kothrb;

public class PatientBot extends Bot{
    public PatientBot(){
        name="PatientBot";
    }
    int turn = 0;
    public Action action(int[][] map){
        switch(turn++){
        case 0: return Action.UP;
        case 1: return Action.MINE;
        case 2: return Action.LEFT;
        case 3: return Action.DOWN;
        case 4: return Action.MINE;
        case 5: return Action.DOWN;
        case 6: return Action.RIGHT;
        case 7: return Action.MINE;
        case 8: return Action.RIGHT;
        case 9: return Action.UP;
        case 10: return Action.MINE;
        default: return Action.PASS;
        }
    }
}