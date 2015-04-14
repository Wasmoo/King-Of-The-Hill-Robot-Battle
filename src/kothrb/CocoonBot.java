package kothrb;
import java.awt.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class CocoonBot extends Bot{
    private static final int Y = 64, X = 64;
    public CocoonBot() {
        this.name="Cocoon";
    }

    private int neighborMines(int[][] map, Point p){
        int mines=0;
        if (p.y>=Y-1||map[p.x][p.y+1]==-1) mines++;
        if (p.y<=  0||map[p.x][p.y-1]==-1) mines++;
        if (p.x>=X-1||map[p.x+1][p.y]==-1) mines++;
        if (p.x<=  0||map[p.x-1][p.y]==-1) mines++;
        return mines;
    }

    @Override
    public Action action(int[][] map) {

        Point firstPoint = new Point(p.x, p.y);
        if ( neighborMines(map,firstPoint) == 4 ) {
            return Action.PASS; // we've built a cocoon and are safe
        }

        // calculate how many mines surround each point of the map, 0-4
        Map<Point, Integer> safety = new HashMap<>();
        for(int x=0; x<X; x++) {
            for(int y=0; y<Y; y++) {
                //TODO: teach the bot that it won't ever lay mines on the edge or adjacent to other mines
                //      so effective safety is 0 if getting to 3 requires one of those actions
                Point p = new Point(x,y);
                int s = neighborMines(map,p);
                safety.put(p,s);
            }
        }

        // flood fill the map to find travel paths and distances from the current square to each other non-mine square
        LinkedBlockingQueue<Point> frontier = new LinkedBlockingQueue<>();
        Map<Point, Point> came_from = new HashMap<>();
        Map<Point, Integer> distance = new HashMap<>();
        frontier.add(firstPoint);
        came_from.put(firstPoint, new Point(-1, -1));
        distance.put(firstPoint,0);
        Point current;
        while((current = frontier.poll()) != null) {
            // TODO: bail here for squares too far away to be worth considering
            for(int d=0; d<4; d++) {
                int dx = d<2?(d*2-1):0; //-1 +1  0  0
                int dy = d<2?0:(d*2-5); // 0  0 -1 +1
                if (current.y+dy<Y && current.y+dy>=0 && current.x+dx<X && current.x+dx>=0) {
                    if (map[current.x+dx][current.y+dy] != -1) {
                        Point next = new Point(current.x+dx, current.y+dy);
                        if(!distance.containsKey(next)) {
                            frontier.add(next);
                            came_from.put(next, current);
                            distance.put(next,distance.get(current)+1);
                        }
                    }
                }
            }
        }

        // calculate the safety of where we might end up after dropping a mine
        // weighted towards safer spaces, we really want to end up in space that's already safety=3
        Map<Point, Integer> safety2 = new HashMap<>();
        for(int x=0; x<X; x++) {
            for(int y=0; y<Y; y++) {
                int s = 0;
                for(int d=0; d<4; d++) {
                    int dx = d<2?(d*2-1):0; //-1 +1  0  0
                    int dy = d<2?0:(d*2-5); // 0  0 -1 +1
                    Point p = new Point(x+dx,y+dy);
                    if ( x+dx<0||x+dx>=X||y+dy<0||y+dy>=Y || map[x+dx][y+dy]==-1 ) {
                        // never risk moving into a mine or off the map!
                        s = -999;
                        break;
                    } else if ( safety.containsKey(p) ) {
                        // safety 3 is great
                        // safety 2 is ok
                        // safety 1 is better than nothing
                        s += safety.get(p)>2?12:safety.get(p)>1?3:safety.get(p); // magic numbers!
                    }
                }
                Point p = new Point(x,y);
                safety2.put(p,s);
            }
        }

        // find the safest/closest point
        Point bestpoint = firstPoint;
        double bestscore = safety2.get(firstPoint)+0.1;
        distance.remove(firstPoint);
        for (Map.Entry<Point,Integer> pd : distance.entrySet()) {
            Point p = pd.getKey();
            double score = safety2.get(p)/Math.pow(pd.getValue(),0.8); // magic number!
            if(score > bestscore) {
                bestscore = score;
                bestpoint = p;
            } else if (score == bestscore) {
                if ( (bestpoint.x-X/2)*(bestpoint.x-X/2)+(bestpoint.y-Y/2)*(bestpoint.y-Y/2) > 
                     (p.x-X/2)*(p.x-X/2)+(p.y-Y/2)*(p.y-Y/2) ) {
                    // aim for the middle, less wasted time at edges
                    bestscore = score;
                    bestpoint = p;
                }
            }
        }

        // we're in the right spot, place a mine and hope for a good random bump
        if(bestpoint == firstPoint) {
            return Action.MINE;
        }

        // walk back up came_from to figure out which way to step to get to bestpoint
        while(came_from.get(bestpoint) != firstPoint) {
            bestpoint = came_from.get(bestpoint);
        }
        if ( bestpoint.x > firstPoint.x ) {
            return Action.RIGHT;
        }
        if ( bestpoint.x < firstPoint.x ) {
            return Action.LEFT;
        }
        if ( bestpoint.y > firstPoint.y ) {
            return Action.DOWN;
        }
        if ( bestpoint.y < firstPoint.y ) {
            return Action.UP;
        }

        // should never happen
        return Action.MINE;
    }

}