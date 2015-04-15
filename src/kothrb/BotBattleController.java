package kothrb;

import java.util.*;

public class BotBattleController {
    
        private static List<Bot> all_bots = new ArrayList();
        private static List<Class> all_classes = new ArrayList();
        
	public static void main(String[] args) throws InstantiationException, IllegalAccessException {
		/*edit and uncomment these lines as needed*/
                all_classes.add(Wanderlust.class);
                all_classes.add(Frozen.class);
                all_classes.add(CocoonBot.class);
                all_classes.add(ProtectiveBot.class);
                all_classes.add(Albert.class);
                all_classes.add(MineThenBoom.class);
                all_classes.add(PatientBot.class);
                
                int botSize = all_classes.size();
                
                ArrayList<List<Class>> all_combos = new ArrayList();
                all_combos.addAll(combinations(all_classes, 2));
                all_combos.addAll(combinations(all_classes, 3));
                all_combos.addAll(combinations(all_classes, 4));
                
                System.out.println(all_combos);
                
                
                int all_wins[] = new int[botSize];
                int all_ties[] = new int[botSize];
                int all_losses[] = new int[botSize];
                String names[] = new String[botSize];
                int runs = 100;
                
                for (List<Class> combo : all_combos) {
                    int wins[] = new int[botSize];
                    int ties[] = new int[botSize];
                    int losses[] = new int[botSize];
                    System.out.println("Score for "+combo+":");
                    for (int i = 0; i < runs; i++) {
                        List<Bot> players = new ArrayList();
                        for (Class c : combo) {
                            Bot b = (Bot)c.newInstance();
                            b.register(players);
                            names[all_classes.indexOf(c)] = b.toString();
                        }
                        List<Bot> winners = runGame(players, false);
                        if (winners.size() == 1) {
                            wins[winners.get(0).id()-1]++;
                        } else {
                            for (Bot b : winners) {
                                ties[b.id()-1]++;
                            }
                        }
                        players.removeAll(winners);
                        for (Bot b : players) {
                            losses[b.id()-1]++;
                        }
                    }
                    for (Class c : combo) {
                        int i = all_classes.indexOf(c);
                        int totalRounds = wins[i] + ties[i] + losses[i];
                        int points = (int)(1000 * Math.max(0, wins[i] + ties[i]/3.0 - losses[i]/8.0) / totalRounds);
                        System.out.println(names[i]+": "+wins[i]+" wins, "+ties[i]+" ties, "+losses[i]+" losses; Score = "+points);
                        all_wins[i] += wins[i];
                        all_ties[i] += ties[i];
                        all_losses[i] += losses[i];
                    }
                    System.out.println("");
                }
                
                System.out.println("Final Score:");
                for (int i = 0; i < botSize; i++) {
                    Bot b = all_bots.get(i);
                    int totalRounds = all_wins[i] + all_ties[i] + all_losses[i];
                    int points = (int)(1000 * Math.max(0, all_wins[i] + all_ties[i]/3.0 - all_losses[i]/8.0) / totalRounds);
                    System.out.println(b+": "+all_wins[i]+" wins, "+all_ties[i]+" ties, "+all_losses[i]+" losses; Score = "+points);
                }
        }
        private static List<Bot> runGame(List<Bot> in_bots, boolean log) {
            List<Bot> bots = new ArrayList(in_bots);
		Random r=new Random(System.currentTimeMillis()%1000);
		int turns=1000;
		int[][] map=new int[64][64];
		for(int x=0;x<64;x++){
			map[x]=new int[64];
		}
		int i=5+r.nextInt(100);
		for(int j=0;j<i;j++){
			int x=r.nextInt(64),y=r.nextInt(64);
			map[x][y]=-1;
		}
		for(Bot b:bots){
			boolean a=true;
			while(a){
				int x=r.nextInt(64),y=r.nextInt(64);
				if(map[x][y]!=0)continue ;
				map[x][y]=b.id();
				b.p=new Bot.Position(x,y);
				a=false;
			}
		}
		if (log) System.out.println("Bots loaded.");
		while(turns>0){
			turns--;
                        ArrayList<Bot> dead = new ArrayList();
			for(Bot b:bots){
				long k=System.nanoTime();
                                Bot.Action s;
                                try {
                                    s=b.action(copy(map));
                                } catch (Exception ex) {
                                    dead.add(b);
                                    continue;
                                }
				k=(System.nanoTime()-k);
				if (log) System.out.println(b+"("+b.p.x+","+b.p.y+"):"+s+" ("+k/1000+"us)");
				k/=1000000;
				Bot.Position p=b.p;
				int x=p.x;
				int y=p.y;
				map[x][y]=0;
				if(k>50){
					dead.add(b);
					if (log) System.out.println(b+" took too long to respond, and has been eliminated on turn "+(1000-turns)+".");
					break ;
				}
				if(s==Bot.Action.MINE){
					s=(new Bot.Action[]{Bot.Action.DOWN,Bot.Action.LEFT,Bot.Action.RIGHT,Bot.Action.UP})[r.nextInt(4)];
					map[x][y]=-1;
				}
				if(s==Bot.Action.LEFT){
					x--;
				}
				if(s==Bot.Action.RIGHT){
					x++;
				}
				if(s==Bot.Action.UP){
					y--;
				}
				if(s==Bot.Action.DOWN){
					y++;
				}
				if(y<0||y>=64||x<0||x>=64){
					dead.add(b);
					if (log) System.out.println(b+" left the map on turn "+(1000-turns)+".");
					break ;
				}
				if(map[x][y]==-1){
					dead.add(b);
					map[x][y]=0;
					if (log) System.out.println(b+" was killed by a mine on turn "+(1000-turns)+".");
					break ;
				}
				if(map[x][y]!=0&&map[x][y]!=b.id()){
					Bot c=all_bots.get(map[x][y]-1);
					dead.add(c);
					if (log) System.out.println(c+" was killed by "+b+" on turn "+(1000-turns)+".");
					map[x][y]=b.id();
					p.x=x;p.y=y;
					break;
				}
				p.x=x;p.y=y;
				map[x][y]=b.id();
			}
                        bots.removeAll(dead);
                        if (bots.size() == 0) {
                            if (log) System.out.println("All bots have lost.");
                            break;
                        } else if (bots.size() == 0) {
                            if (log) System.out.println(bots.get(0)+" has won in "+(1000-turns)+" steps.");
                            break;
                        }
		}
		if(turns==0)if (log) System.out.println("Time ran out. "+bots+" are still standing.");
                return bots;
	}
	public static int[][] copy(int[][] a){
		int[][] o=new int[64][64];
		for(int x=0;x<64;x++){
			for(int y=0;y<64;y++){
				o[x][y]=a[x][y];
			}
		}
		return o;
	}
        private static <X> ArrayList<ArrayList<X>> combinations(List<X> list, int count) {
            ArrayList<ArrayList<X>> ret = new ArrayList();
            if (count == 0 || count > list.size()) {
                return ret;
            } else if (count == 1) {
                for (X x : list) {
                    ret.add(new ArrayList(Arrays.asList(x)));
                }
            } else {
                ArrayList<ArrayList<X>> combos = combinations(list, count-1);
                for (List<X> combo : combos) {
                    ArrayList<X> options = new ArrayList(list);
                    options.removeAll(combo);
                    for (X x : options) {
                        ArrayList<X> retList = new ArrayList(combo);
                        retList.add(x);
                        ret.add(retList);
                    }
                }
                for (int i = ret.size()-1; i >= 0; i--) {
                    List<X> t = ret.get(i);
                    for (int j = i-1; j >= 0; j--) {
                        if (ret.get(j).containsAll(t)) {
                            ret.remove(i);
                            break;
                        }
                    }
                    
                }
            }
            return ret;
        }
    
}