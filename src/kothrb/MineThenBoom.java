package kothrb;
import java.util.*;


public class MineThenBoom extends Bot{
    public MineThenBoom() {
        this.name="MineThenBoom";
    }
    public Action action(int[][] map) {
        List<Position> enemies=new ArrayList<Position>();
        for(int x=1;x<map.length;x++){
            for(int y=1;y<map[x].length;y++){
                if(map[x][y]!=0&&map[x][y]!=this.id()&&map[x][y]!=-1){
                    enemies.add(new Position(x,y));
                }
            }
        }
        double d=200;
        Position x=null;
        for(Position pos:enemies){
            if(p.distance(pos)<d){
                x=pos;
                d=p.distance(pos);
            }
        }
    // initialize checking for walls
    boolean can_move_left = p.x != 0;
    boolean can_move_right = p.x != 63;
    boolean can_move_up = p.y != 0;
    boolean can_move_down = p.y != 63;
    // Maybe I'll throw these in, they were in my old code
    int nearby_players = 0;
    int close_players = 0;
    int crush_players = 0;
    int diag_players = 0;
    int danger_players = 0;
    // Check if there's anyone nearby to crush, if it won't put us in danger.
        if(x.x<p.x&&map[p.x-1][p.y]!=-1&&x.x<p.x&&map[p.x-1][p.y]!=0) {
       boolean no_one_around = map[p.x-1][p.y-1] < 1 && map[p.x-1][p.y+1] < 1;
       if(no_one_around)return Action.LEFT;
    }
        if(x.x>p.x&&map[p.x+1][p.y]!=-1&&x.x<p.x&&map[p.x-1][p.y]!=0) {
       boolean no_one_around = map[p.x+1][p.y-1] < 1 && map[p.x+1][p.y+1] < 1;
       if(no_one_around)return Action.RIGHT;
    }
        if(x.y>p.y&&map[p.x][p.y+1]!=-1&&x.x<p.x&&map[p.x-1][p.y]!=0) {
       boolean no_one_around = map[p.x-1][p.y+1] < 1 && map[p.x+1][p.y+1] < 1;
       if(no_one_around)return Action.DOWN;
    }
        if(x.y<p.y&&map[p.x][p.y-1]!=-1&&x.x<p.x&&map[p.x-1][p.y]!=0) {
       boolean no_one_around = map[p.x-1][p.y-1] < 1 && map[p.x+1][p.y-1] < 1;
       if(no_one_around)return Action.UP;
    }
    // If we've come this far, it means no one is crushable. Lay a mine, but only if we're ABSOLUTELY safe.
    boolean no_one_around = map[p.x+1][p.y-1] < 1 && map[p.x+1][p.y+1] < 1 && map[p.x-1][p.y-1] < 1 && map[p.x-1][p.y+1] < 1;
    boolean not_walking_into_mine = map[p.x+1][p.y] == 0 && map[p.x-1][p.y] == 0 && map[p.x][p.y-1] == 0 && map[p.x][p.y+1] == 0;
    boolean not_walking_near_danger = map[p.x+2][p.y] < 1 && map[p.x-2][p.y] < 1 && map[p.x][p.y+2] < 1 && map[p.x][p.y-2] < 1;
    if(no_one_around && not_walking_into_mine && not_walking_near_danger)return Action.MINE;
    // We clearly are pinned down by some number of players on one or more edges. Let's make our way out of here!
    no_one_around = map[p.x-1][p.y-1] < 1 && map[p.x-1][p.y+1] < 1 && map[p.x-2][p.y] < 1 && map[p.x-1][p.y] == 0 && x.x<p.x;
    if(no_one_around)return Action.LEFT;
    no_one_around = map[p.x+1][p.y-1] < 1 && map[p.x+1][p.y+1] < 1 && map[p.x+2][p.y] < 1 && map[p.x+1][p.y] == 0 && x.x>p.x;
    if(no_one_around)return Action.RIGHT;
    no_one_around = map[p.x-1][p.y-1] < 1 && map[p.x+1][p.y-1] < 1 && map[p.x][p.y-2] < 1 && map[p.x][p.y-1] == 0 && x.y<p.y;
    if(no_one_around)return Action.DOWN;
    no_one_around = map[p.x-1][p.y+1] < 1 && map[p.x+1][p.y+1] < 1 && map[p.x][p.y+2] < 1 && map[p.x][p.y+1] == 0 && x.y>p.y;
    if(no_one_around)return Action.UP;
    // Well then. We can't crush, lay a mine, or escape.
    // We are most likely near a player who, if they move, will either no longer be a threat or we can crush.
    // If we are somehow surrounded by mines, we're safe anyways.
    return Action.PASS; 

    }
}