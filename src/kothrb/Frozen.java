package kothrb;

    public class Frozen extends Bot{
            public Frozen(){
                this.name="Frozen";
            }
            public Action action(int[][] map) {
                if (p.x > 0 && map[p.x-1][p.y] > 0) return Action.LEFT;
                if (p.x < map.length-1 && map[p.x+1][p.y] > 0) return Action.RIGHT;
                if (p.y > 0 && map[p.x][p.y-1] > 0) return Action.UP;
                if (p.y < map.length-1 && map[p.x][p.y+1] > 0) return Action.DOWN;
                return Action.PASS;
            }
    }