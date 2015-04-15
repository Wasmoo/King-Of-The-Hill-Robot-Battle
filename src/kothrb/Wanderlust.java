package kothrb;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Wanderlust extends Bot {

    private Random random;

    public Wanderlust() {
        this.name = "Wanderlust";
        this.random = new Random(System.currentTimeMillis());
    }

    @Override
    public Action action(int[][] map) {

        List<Action> actions = new ArrayList<Action>();
        List<Position> enemyPositions = mapEnemyPositions(map);
        Position nearestBotPosition = getNearestBotPosition(enemyPositions);
        Action action = Action.PASS;

        if (nearestBotPosition != null && nearestBotPosition.distance(this.p) < 10) {
        // Run! Can't risk to drop a mine because of the random movement afterwards.
            // So check where the enemy is and run the opposite way!

            int dx = this.p.x - nearestBotPosition.x;
            int dy = this.p.y - nearestBotPosition.y;

            Action horizontalAction = dx < 0 ? Action.LEFT : Action.RIGHT;
            Action verticalAction = dy < 0 ? Action.UP : Action.DOWN;

            // Is the enemy closer to me on the x-axis, or the y-axis?
            if (Math.abs(dx) < Math.abs(dy)) {
            // OK, which way do we run away from the enemy? Left or right?
                // And if we can't run left or right, we should think about a back-up plan: moving vertically.
                Action primaryAction = dx < 0 ? Action.LEFT : Action.RIGHT;
                actions.add(primaryAction);
                actions.add(verticalAction);
                action = getValidMove(actions, true, map);
            } else {
            // OK, which way do we run away from the enemy? Up or down?
                // And if we can't run up or down, we should think about a back-up plan: moving horizontally.
                Action primaryAction = dy < 0 ? Action.UP : Action.DOWN;
                actions.add(primaryAction);
                actions.add(horizontalAction);
                action = getValidMove(actions, true, map);
            }
        } else {
            // Just casual strolling. Hey, perhaps dropping a mine? #casualThings
            actions.add(Action.LEFT);
            actions.add(Action.RIGHT);
            actions.add(Action.UP);
            actions.add(Action.DOWN);
            actions.add(Action.MINE);

            if (this.p.x == 0 || this.p.x == 63 || this.p.y == 0 || this.p.y == 63 || getNumberOfMinesAroundPosition(this.p, map) > 0) {
                actions.remove(Action.MINE);
            }

            action = getValidMove(actions, false, map);
        }

        return action;
    }

    private Action getValidMove(List<Action> actions, boolean preserveOrder, int[][] map) {

        List<Action> validMoves = new ArrayList<Action>();

        for (Action action : actions) {
            if (action == Action.LEFT && this.p.x > 0 && map[this.p.x - 1][this.p.y] == 0) {
                validMoves.add(Action.LEFT);
            }

            if (action == Action.RIGHT && this.p.x < 63 && map[this.p.x + 1][this.p.y] == 0) {
                validMoves.add(Action.RIGHT);
            }

            if (action == Action.UP && this.p.y > 0 && map[this.p.x][this.p.y - 1] == 0) {
                validMoves.add(Action.UP);
            }

            if (action == Action.DOWN && this.p.y < 63 && map[this.p.x][this.p.y + 1] == 0) {
                validMoves.add(Action.DOWN);
            }

            if (action == Action.MINE) {
                validMoves.add(Action.MINE);
            }
        }

        if (validMoves.isEmpty()) {
            return Action.PASS;
        }

        return preserveOrder ? validMoves.get(0) : validMoves.get(random.nextInt(validMoves.size()));
    }

    private int getNumberOfMinesAroundPosition(Position position, int[][] map) {
        int count = 0;

        if (position.x > 0 && map[position.x - 1][position.y] == -1) {
            count++;
        }

        if (position.x < 63 && map[position.x + 1][position.y] == -1) {
            count++;
        }

        if (position.y > 0 && map[position.x][position.y - 1] == -1) {
            count++;
        }

        if (position.y < 63 && map[position.x][position.y + 1] == -1) {
            count++;
        }

        return count;
    }

    private Position getNearestBotPosition(List<Position> enemyPositions) {
        Position botPosition = null;
        double distance = Double.MAX_VALUE;

        for (Position position : enemyPositions) {
            double botDistance = position.distance(this.p);
            if (botDistance <= distance) {
                distance = botDistance;
                botPosition = position;
            }
        }

        return botPosition;
    }

    private List<Position> mapEnemyPositions(int[][] map) {

        List<Position> enemyPositions = new ArrayList<Position>();

        for (int yy = 0; yy < 63; yy++) {
            for (int xx = 0; xx < 63; xx++) {
                if (map[xx][yy] > 0 && map[xx][yy] != id()) {
                    enemyPositions.add(new Position(xx, yy));
                }
            }
        }

        return enemyPositions;
    }
}
