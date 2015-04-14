package kothrb;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A bot that builds a cocoon to live in, and protects its entrance at all costs.
 * Falls back to a retreat pattern if this is impossible.
 *
 * @author j_selby
 */
public class ProtectiveBot extends Bot {
    private static final int CLOSE_INIT_MARGIN = 15;
    private static final int AGGRO_TURN = 700;
    private static final int MAX_HOLDING_TURNS = 15;

    // Cheeky, but not against rules
    private static java.util.List<ProtectiveBot> saveOurKind = new ArrayList<>();

    private State state = State.INIT;
    private Map<State, Bot> botMaps = new HashMap<>();
    private Random r = new Random();

    private int homeX;
    private int homeY;
    private int attackX;
    private int attackY;
    private int turn;
    private int holdingTurns;
    private Action lastmove = Action.PASS;

    private boolean killSwitch;

    private PathfindingThread pathfindingThread;
    private int pathfindingTicks;
    private long tickStart;

    public ProtectiveBot() {
        saveOurKind.add(this);
        this.name = "ProtectiveBot";

        // Build bots
        for (State state : State.values()) {
            // Workaround inability to create new instances
            // inside the enum itself due to static nature
            switch(state) {
                case INIT:
                    botMaps.put(state, new InitStage());
                    break;
                case BUILDING:
                    botMaps.put(state, new BuildingStage());
                    break;
                case MOVING_GUARD:
                    botMaps.put(state, new MovingToGuardStage());
                    break;
                case GUARD:
                    botMaps.put(state, new GuardStage());
                    break;
                case AGGRO:
                    botMaps.put(state, new AggroBot());
                    break;
                case STALEMATE_RESOLUTION:
                    botMaps.put(state, new StalemateResolutionBot());
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return "PB{s=" + state.name() + ",m=" + lastmove.name() + "}";
    }

    @Override
    public Action action(int[][] map) {
        tickStart = System.currentTimeMillis();
        turn++;

        Bot b = botMaps.get(state);
        b.p = p;
        b.name = name;

        Action action = b.action(map);
        if (!killSwitch && !safeAction(map, action, false)) {
            // Last ditch bail
            System.out.println("ProtectiveBot: Unsafe move from " + b.getClass().getName());
            action = randomSafeDir(map);
        }

        lastmove = action;
        if (state != State.AGGRO || pathfindingThread != null) {
            holdingTurns = 0;
        } else if (action == Action.PASS) {
            holdingTurns++;
        } else {
            holdingTurns = 0;
        }
        return action;
    }

    private Action pathfind(int x, int y, int[][] map) {
        if (pathfindingTicks > 20) {
            System.out.println("ProtectiveBot: Pathfinding timed out. Killing, and relaunching.");
            try {
                pathfindingThread.stop();
            } catch (Exception ignored) {}
            pathfindingThread = null;
            pathfindingTicks = 0;
        }

        if (pathfindingThread == null) {
            // We are ready to spin up an instance!
            pathfindingThread = new PathfindingThread(p.x, p.y, x, y, map);
            pathfindingThread.setName("ProtectiveBot Pathfinding Thread"); // For debugging etc
            pathfindingThread.start();
        }

        try {
            // Allow 25 milliseconds this tick for a result.
            int waitTime = 40 - (int) Math.max(0, System.currentTimeMillis() - tickStart);
            //System.out.println("Tick timer: " + waitTime);
            if (pathfindingThread.counter.await(waitTime, TimeUnit.MILLISECONDS)) {
                // We have a result!
                Action result = pathfindingThread.result;
                pathfindingThread = null;
                pathfindingTicks = 0;
                return result;
            } else {
                // Wait a turn
                // TODO: Alternate pathfinding here if main times out && if others nearby
                pathfindingTicks++;
                return Action.PASS;
            }
        } catch (InterruptedException e) {
            // Interrupted?
            System.err.println("ProtectiveBot: What the... I was interrupted while processing stuff...");
            e.printStackTrace();
            return Action.PASS;
        }
    }

    private Action pathfind(int rootX, int rootY, int x, int y, int[][] map) {
        // A* pathfinding
        // Thread safe
        List<Point> open = new ArrayList<>();
        List<Point> closed = new ArrayList<>();
        Map<Point, Point> parents = new HashMap<>();

        open.add(new Point(rootX, rootY));

        int movementCost = 0;
        Point targetPoint = null;

        while(open.size() > 0) {
            // Fetch lowest F score
            Point lowestPoint = null;
            int lowestScore = Integer.MAX_VALUE;
            for (Point checkPoint : open) {
                if (checkPoint == null) {
                    continue;
                }

                int gScore = movementCost + 10; // We are adding a cost here for this tile
                int hScore = 10 * (Math.abs(checkPoint.x - x) + Math.abs(checkPoint.y - y));
                int fScore = gScore + hScore;
                if (fScore < lowestScore) {
                    lowestPoint = checkPoint;
                    lowestScore = fScore;
                }
            }

            if (lowestPoint == null) {
                break;
            }

            // Check if point matches
            if (lowestPoint.x == x && lowestPoint.y == y) {
                // We found our targetPoint
                targetPoint = lowestPoint;
                break;
            }

            // Add to closed list, remove from open
            open.remove(lowestPoint);
            closed.add(lowestPoint);

            // Add friends
            Point[] friends = validNeighbours(map, lowestPoint);
            for (Point point : friends) {
                if (!closed.contains(point)) {
                    open.add(point);
                    // TODO: Recalcuate F (G + H) scores if required for better path
                    parents.put(point, lowestPoint);
                }
            }

            // Add to the movement cost for future tiles
            movementCost += 10;
        }

        if (targetPoint == null) {
            // Cannot pathfind.
            return randomSafeDir(map);
        }

        // Build a path, get last index
        Point moveToPoint = targetPoint;
        while(true) {
            // Get parent
            Point parent = parents.get(moveToPoint);
            if (parent == null) {
                // WTF? No parent
                System.out.println("ProtectiveBot: No parent for node " + moveToPoint + ". I was at " + rootX + ":" + rootY);
                break;
            }
            if (parent.x == rootX && parent.y == rootY) {
                // We are back home
                break;
            }
            moveToPoint = parent;
        }

        // Get diff
        int relX = moveToPoint.x - rootX;
        int relY = moveToPoint.y - rootY;

        Action action = Action.PASS;
        if (relX > 0) {
            action = Action.RIGHT;
        } else if (relX < 0) {
            action = Action.LEFT;
        } else if (relY > 0) {
            action = Action.DOWN;
        } else if (relY < 0) {
            action = Action.UP;
        }

        if (!safeAction(map, action, false)) {
            return Action.PASS;
        }

        return action;
    }

    private Point[] validNeighbours(int[][] map, Point current) {
        Point[] array = new Point[4];
        if (safePosition(map, current.x + 1, current.y, true)) {
            array[0] = new Point(current.x + 1, current.y);
        }
        if (safePosition(map, current.x - 1, current.y, true)) {
            array[1] = new Point(current.x - 1, current.y);
        }
        if (safePosition(map, current.x, current.y + 1, true)) {
            array[2] = new Point(current.x, current.y + 1);
        }
        if (safePosition(map, current.x, current.y - 1, true)) {
            array[3] = new Point(current.x, current.y - 1);
        }
        return array;
    }

    private Action randomUNSAFEDir(int[][] map) {
        int i = r.nextInt(4);
        if (i == 0) {
            return Action.UP;
        } else if (i == 1) {
            return Action.DOWN;
        } else if (i == 2) {
            return Action.LEFT;
        } else if (i == 3) {
            return Action.RIGHT;
        } else {
            return Action.MINE;
        }
    }

    private Action randomSafeDir(int[][] map) {
        Action action = Action.LEFT;
        for (int attempt = 0; attempt < 100; attempt++) {
            int i = r.nextInt(4);
            if (i == 0) {
                action = Action.UP;
            } else if (i == 1) {
                action = Action.DOWN;
            } else if (i == 2) {
                action = Action.LEFT;
            } else if (i == 3) {
                action = Action.RIGHT;
            }

            if (safeAction(map, action, false)) {
                return action;
            }
        }

        // We are trapped...
        return Action.PASS;
    }

    public boolean safeAction(int[][] map, Action action, boolean ignoreOtherPlayers) {
        int moveX = 0, moveY = 0;

        // Get the relative movement from this
        switch (action) {
            case UP:
                moveY--;
                break;
            case DOWN:
                moveY++;
                break;
            case LEFT:
                moveX--;
                break;
            case RIGHT:
                moveX++;
                break;
        }

        int absX = p.x + moveX;
        int absY = p.y + moveY;

        // If this is a mine, make sure its safe
        if (action == Action.MINE) {
            if (absX < 1 || absX > map.length - 2 ||
                    absY < 1 || absY > map[absX].length - 2) {
                // It might chuck us out of the map
                return false;
            }
            if (!safePosition(map, absX + 1, absY, ignoreOtherPlayers)
                    || !safePosition(map, absX - 1, absY, ignoreOtherPlayers)
                    || !safePosition(map, absX, absY + 1, ignoreOtherPlayers)
                    || !safePosition(map, absX, absY - 1, ignoreOtherPlayers)) {
                // We could fall on a mine
                return false;
            }
        }

        return safePosition(map, absX, absY, ignoreOtherPlayers);
    }

    public boolean safePosition(int[][] map, int x, int y, boolean ignoreOtherPlayers) {
        if (x < 0 || x > map.length - 1) {
            return false;
        } else if (y < 0 || y > map.length - 1) {
            return false;
        }

        // Check that there are no bots waiting at the corners here
        if (!ignoreOtherPlayers) {
            if (x > 0) {
                // Check left
                if (!(x - 1 == p.x && y == p.y) && map[x - 1][y] > 0) {
                    return false;
                }
            }
            if (y > 0) {
                // Check top
                if (!(x == p.x && y - 1== p.y) && map[x][y - 1] > 0) {
                    return false;
                }
            }
            if (x < map.length - 1) {
                // Check right
                if (!(x + 1 == p.x && y == p.y) && map[x + 1][y] > 0) {
                    return false;
                }
            }
            if (y < map[x].length - 1) {
                // Check top
                if (!(x == p.x && y + 1== p.y) && map[x][y + 1] > 0) {
                    return false;
                }
            }
        }

        return map[x][y] != -1;
    }

    private class InitStage extends Bot {
        @Override
        public Action action(int[][] map) {
            // Check our distance to other bots
            double closestDistance = Double.MAX_VALUE;

            for (int x = 0, mapLength = map.length; x < mapLength; x++) {
                int[] xMap = map[x];
                for (int y = 0, xMapLength = xMap.length; y < xMapLength; y++) {
                    int point = xMap[y];
                    if (x == p.x && y == p.y) {
                        // Ignore this bot
                        continue;
                    }
                    if (point > 0) {
                        // Found another bot
                        double distance = Math.hypot(p.x - x, p.y - y);
                        if (Math.abs(distance) < Math.abs(closestDistance)) {
                            closestDistance = distance;
                        }
                    }
                }
            }

            if (closestDistance < CLOSE_INIT_MARGIN) {
                // Unable to build our cocoon, kill all instead
                state = State.AGGRO;
                return ProtectiveBot.this.action(map);
            }

            // Simply an init bot, kill it
            state = State.BUILDING;
            return ProtectiveBot.this.action(map);
        }
    }

    private class BuildingStage extends Bot {
        private int[][] buildPoints = new int[][] {
                {0, 1, 0},
                {8, 2, 1},
                {0, 1, 0}
        }; // Where 1 is a mine, 2 is our preferred location, 0 is air/whatever, 8 is where the danger is
        // Rotate 90 degrees clockwise for actual in-game build.

        private Point currentPoint;

        private int baseX;
        private int baseY;

        private boolean init = true;

        @Override
        public Action action(int[][] map) {
            if (init) {
                init = false;
                // First run
                baseX = p.x;
                baseY = p.y;

                // Make sure we don't fall out of map
                if (baseX > map.length - 3) {
                    baseX -= 3;
                }
                if (baseX == 0) {
                    baseX+=2;
                } else if (baseX == 1) {
                    baseX++;
                }
                if (baseY > map.length - 4) {
                    baseY -= 4;
                }
                if (baseY == 0) {
                    baseY+=2;
                } else if (baseY == 1) {
                    baseY++;
                }
            }

            if (currentPoint == null) {
                // Find ourselves a point
                for (int x = 0; x < buildPoints.length; x++) {
                    if (currentPoint != null) {
                        break;
                    }
                    for (int y = 0; y < buildPoints[x].length; y++) {
                        if (buildPoints[x][y] == 1) {
                            currentPoint = new Point(baseX + x, baseY + y);
                            buildPoints[x][y] = 0;
                            break;
                        } else if (buildPoints[x][y] == 2) {
                            homeX = baseX + x;
                            homeY = baseY + y;
                        } else if (buildPoints[x][y] == 8) {
                            attackX = baseX + x;
                            attackY = baseY + y;
                        }
                    }
                }

                if (currentPoint == null) {
                    // Yay! We did it. Move to guard spot
                    state = State.MOVING_GUARD;
                    return ProtectiveBot.this.action(map);
                }
            }

            if (p.x == currentPoint.x && p.y == currentPoint.y) {
                // Move on to next point
                currentPoint = null;
                if (!safeAction(map, Action.MINE, false)) {
                    return randomSafeDir(map);
                } else {
                    return Action.MINE;
                }
            }

            if (!safePosition(map, currentPoint.x, currentPoint.y, false)) {
                // Whatever.
                currentPoint = null;
                return action(map);
            }

            return pathfind(currentPoint.x, currentPoint.y, map);
        }

    }

    private class MovingToGuardStage extends Bot {
        @Override
        public Action action(int[][] map) {
            if (p.x == homeX && p.y == homeY) {
                state = State.GUARD;
                return ProtectiveBot.this.action(map);
            }
            if (!safePosition(map, homeX, homeY, false)) {
                // Well, home isn't safe anymore. Go aggro
                state = State.AGGRO;
                return ProtectiveBot.this.action(map);
            }
            return pathfind(homeX, homeY, map);
        }
    }

    private class GuardStage extends Bot {
        @Override
        public Action action(int[][] map) {
            if (map[attackX][attackY] > 0) {
                state = State.MOVING_GUARD;
                return Action.UP;
            }

            // YOLO mode, if turn > 900, or only bots left are us, or one other
            int countBots = 0;
            for (int x = 0, mapLength = map.length; x < mapLength; x++) {
                int[] xMap = map[x];
                for (int y = 0, xMapLength = xMap.length; y < xMapLength; y++) {
                    int point = xMap[y];
                    if (point > 0) {
                        // Found another bot
                        countBots++;
                    }
                }
            }

            int friendlyBotsCount = 0;
            for (Bot bot : saveOurKind) {
                if (!(bot == null || bot.p == null || (map[bot.p.x][bot.p.y] == 0))) {
                    friendlyBotsCount++;
                }
            }

            if (countBots == friendlyBotsCount || turn > AGGRO_TURN || countBots - 1 == friendlyBotsCount) {
                state = State.AGGRO;
            }

            return Action.PASS;
        }
    }

    private class AggroBot extends Bot {
        private List<Point> alertedToUnfairPlay = new ArrayList<>();

        @Override
        public Action action(int[][] map) {
            if (holdingTurns > MAX_HOLDING_TURNS) {
                System.out.println("ProtectiveBot: Stalemate detected.");
                System.out.println("ProtectiveBot: https://youtu.be/iHeLDXtvOCQ?t=2m2s");
                state = State.STALEMATE_RESOLUTION;
                return ProtectiveBot.this.action(map);
            }

            double closestDistance = Double.MAX_VALUE;
            int closestX = 0;
            int closestY = 0;
            boolean closestIsFriend = false;

            // Get target bot
            for (int x = 0, mapLength = map.length; x < mapLength; x++) {
                int[] xMap = map[x];
                for (int y = 0, xMapLength = xMap.length; y < xMapLength; y++) {
                    int point = xMap[y];
                    if (x == p.x && y == p.y) {
                        // Ignore this bot
                        continue;
                    }
                    if (point > 0) {
                        // Make sure they aren't trapped
                        if (!alertedToUnfairPlay.contains(new Point(x, y))
                                && (x == 0 || map[x - 1][y] == -1)
                                && (y == 0 || map[x][y - 1] == -1)
                                && (x == map.length - 1 || map[x + 1][y] == -1)
                                && (y == map.length - 1 || map[x][y + 1] == -1)) {
                            System.out.println("ProtectiveBot: other bot @ "
                                    + x + ":" + y + " has trapped themselves. Unable to pathfind.");
                            alertedToUnfairPlay.add(new Point(x, y));
                            continue;
                        }

                        // Found another bot
                        double distance = Math.hypot(p.x - x, p.y - y);

                        // Check if he is a friend
                        boolean thisIsAFriend = false;
                        for (Bot b : saveOurKind) {
                            if (b != null && b.p != null && b.p.x == x && b.p.y == y) {
                                thisIsAFriend = true;
                            }
                        }

                        if (closestDistance == Double.MAX_VALUE) {
                            // First call
                            if (Math.abs(distance) < Math.abs(closestDistance)) {
                                closestX = x;
                                closestY = y;
                                closestDistance = distance;
                                closestIsFriend = thisIsAFriend;
                            }
                        } else if (closestIsFriend && !thisIsAFriend) {
                            // Go after someone else
                            closestX = x;
                            closestY = y;
                            closestDistance = distance;
                            closestIsFriend = thisIsAFriend;
                        } else {
                            // Don't care less
                            if (Math.abs(distance) < Math.abs(closestDistance)) {
                                closestX = x;
                                closestY = y;
                                closestDistance = distance;
                                closestIsFriend = thisIsAFriend;
                            }
                        }
                    }
                }
            }

            // If there is no bot to path to, go away
            if (closestDistance == Double.MAX_VALUE) {
                return Action.PASS;
            }

            if (closestDistance == 2) {
                // Check if there is a mine inbetween
                boolean isMine = false;
                if (closestX > p.x && !safePosition(map, p.x + 1, p.y, false)) {
                    isMine = true;
                } else if (closestX < p.x && !safePosition(map, p.x - 1, p.y, false)) {
                    isMine = true;
                } else if (closestY < p.y && !safePosition(map, p.x, p.y - 1, false)) {
                    isMine = true;
                } else if (closestY > p.y && !safePosition(map, p.x, p.y + 1, false)) {
                    isMine = true;
                }
                if (closestIsFriend) {
                    // Flipping the kill switch.
                    // It was nice knowing you
                    killSwitch = true;
                    return randomUNSAFEDir(map);
                }
                if (isMine) {
                    // isMine: Use this to our advantage
                    // closestIsFriend: Fall on the sword for our ally
                    return pathfind(closestX, closestY, map);
                }
                // Skip a turn so they move into our hands
                return Action.PASS;
            }

            return pathfind(closestX, closestY, map);

        }
    }

    private class StalemateResolutionBot extends Bot {
        @Override
        public Action action(int[][] map) {
            // TODO: Make sure this bot isn't being targeted by any more then 1 bot, else skip back to aggro.

            // TODO: Trick other bot
            // e.g Albert has a potential exploit in mine placement.
            return Action.PASS;
        }
    }

    /**
     * A enum of all the possible stages our bot can be in.
     */
    private enum State {
        /**
         * We haven't done our initial calculations yet
         */
        INIT,
        /**
         * We are building our fortress
         */
        BUILDING,
        /**
         * We are moving to our guard location
         */
        MOVING_GUARD,
        /**
         * We are guarding
         */
        GUARD,
        /**
         * Aggro mode if we can't build our fortress
         */
        AGGRO,
        /**
         * Solves stalemates.
         */
        STALEMATE_RESOLUTION;
    }

    /**
     * Threads aren't against rules.
     */
    private class PathfindingThread extends Thread {
        private final int rootX;
        private final int rootY;
        private final int targetX;
        private final int targetY;
        private final int[][] map;
        public CountDownLatch counter = new CountDownLatch(1);
        public Action result;

        public PathfindingThread(int rootX, int rootY, int targetX, int targetY, int[][] map) {
            this.rootX = rootX;
            this.rootY = rootY;
            this.targetX = targetX;
            this.targetY = targetY;
            this.map = map;
        }

        @Override
        public void run() {
            // Awaiting orders
            result = pathfind(rootX, rootY, targetX, targetY, map);
            counter.countDown();
        }
    }
}