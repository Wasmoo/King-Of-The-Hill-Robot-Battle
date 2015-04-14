package kothrb;

import java.util.*;

public abstract class Bot {
    public static enum Action{
        UP,DOWN,LEFT,RIGHT,MINE,PASS;
    }
    public static final class Position{
        public int x;
        public int y;
        public String toString(){
            return "("+x+","+y+")";
        }
        public double distance(Position p){
            int dx=p.x-this.x;
            int dy=p.y-this.y;
            return Math.sqrt(dx*dx+dy*dy);
        }
        public Position(int x,int y){
            this.x=x;
            this.y=y;
        }
    }
    public String toString(){return name;}
    public Position p;
    public String name="";
    public abstract Action action(int[][] map);
    private int bot_id;
    public final int id(){return bot_id;}
    public final void register(List<Bot> a){
        a.add(this);
        bot_id=a.size();
    }
}