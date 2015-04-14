package kothrb;
import java.util.*;

public class Albert extends Bot{
    private Random r=new Random();
    public Albert(){
        this.name="Albert";
    }
    private int countMines(int[][] map,Position p){
        int mines=0;
        if(p.y==63||map[p.x][p.y+1]==-1)mines++;
        if(p.y==0||map[p.x][p.y-1]==-1)mines++;
        if(p.x==0||map[p.x-1][p.y]==-1)mines++;
        if(p.x==63||map[p.x+1][p.y]==-1)mines++;
        return mines;
    }
    @Override
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
        if(countMines(map,p)==0&&r.nextDouble()<0.5&&d>2.01)return Action.MINE;
        if(d>1.03&&d<3)return Action.PASS;
        if(x==null)return Action.PASS;
        if(x.x<p.x&&map[p.x-1][p.y]!=-1&&countMines(map,new Position(p.x-1,p.y))<2&&r.nextDouble()<0.8)return Action.LEFT;
        if(x.x>p.x&&map[p.x+1][p.y]!=-1&&countMines(map,new Position(p.x+1,p.y))<2&&r.nextDouble()<0.8)return Action.RIGHT;
        if(x.y>p.y&&map[p.x][p.y+1]!=-1&&countMines(map,new Position(p.x,p.y+1))<2&&r.nextDouble()<0.8)return Action.DOWN;
        if(x.y<p.y&&map[p.x][p.y-1]!=-1&&countMines(map,new Position(p.x,p.y-1))<2&&r.nextDouble()<0.8)return Action.UP;
        List<Action> v=new ArrayList<Action>();
        if(p.y>0&&map[p.x][p.y-1]!=-1&&countMines(map,new Position(p.x,p.y-1))<3)v.add(Action.UP);
        if(p.y<63&&map[p.x][p.y+1]!=-1&&countMines(map,new Position(p.x,p.y+1))<3)v.add(Action.DOWN);
        if(p.x>0&&map[p.x-1][p.y]!=-1&&countMines(map,new Position(p.x-1,p.y))<3)v.add(Action.LEFT);
        if(p.x<63&&map[p.x+1][p.y]!=-1&&countMines(map,new Position(p.x+1,p.y))<3)v.add(Action.RIGHT);
        v.add(Action.PASS);
        return v.get((new Random()).nextInt(v.size()));
    }
}