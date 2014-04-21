import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.ListIterator;

class PseudorandomGenerator {
	final static int MOD = Integer.MAX_VALUE;
	final static int MUL = 48271;
	final static int KEEP = 20;

	LinkedList<Integer> next = new LinkedList<Integer>();
	int range, last;

	public int getNextColor() {
		int ans = next.poll();
		last = (int)(((long) last * MUL) % MOD);
		next.add(last % range);
		return ans;
	}

	public PseudorandomGenerator(int seed, int range) {
		this.range = range;

		int cur = seed;
		for (int i=0; i < KEEP; i++) {
			last = cur;
			next.add(cur % range);
			cur = (int)(((long)cur * MUL) % MOD);
		}
	}
}

class TestCase {
	final int MIN_COLORS = 4, MAX_COLORS = 6;
	final int MIN_N = 8, MAX_N = 16;

	int colors;
	int N;
	int startSeed;
	String[] board;

	public TestCase(long seed) {
		SecureRandom rnd = null;

		try {
			rnd = SecureRandom.getInstance("SHA1PRNG");
		} catch (Exception e) {
			System.err.println("ERROR: unable to generate test case.");
			System.exit(1);
		}

		rnd.setSeed(seed);

		colors = rnd.nextInt(MAX_COLORS - MIN_COLORS + 1) + MIN_COLORS;
		N = rnd.nextInt(MAX_N - MIN_N + 1) + MIN_N;
		startSeed = rnd.nextInt(PseudorandomGenerator.MOD - 1) + 1;

		board = new String[N];
		for (int i=0; i < N; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j=0; j < N; j++) {
				sb.append((char)((int)'0' + rnd.nextInt(colors)));
			}
			board[i] = sb.toString();
		}
	}
}

class World {
	final static int[] DR = new int[] {-1, 0, 1, 0};
	final static int[] DC = new int[] {0, 1, 0, -1};

	int N;
	int[][] board;

	PseudorandomGenerator pg;

	int score, curMove;

	int removeR = -1, removeC = -1;

	int exchangeR1 = -1, exchangeC1 = -1;
	int exchangeR2 = -1, exchangeC2 = -1;

	final Object worldLock = new Object();
	SquareRemoverVis parent;

	public World(TestCase tc, SquareRemoverVis parent) {
		this.parent = parent;

		N = tc.N;
		board = new int[N][N];
		for (int i=0; i < N; i++) {
			for (int j=0; j < N; j++) {
				board[i][j] = tc.board[i].charAt(j) - '0';
			}
		}
		pg = new PseudorandomGenerator(tc.startSeed, tc.colors);
	}

	public void removeSquares() {
		while (true) {
			boolean find = false;

			for (int i=0; i + 1 < N && !find; i++) {
				for (int j=0; j + 1 < N && !find; j++) {
					if (board[i][j] == board[i][j + 1] &&
							board[i][j] == board[i + 1][j] &&
							board[i][j] == board[i + 1][j + 1]) {
						find = true;

						synchronized (worldLock) {
							removeR = i;
							removeC = j;
						}

						parent.updateDrawer();

						synchronized (worldLock) {
							removeR = -1;
							removeC = -1;

							score++;

							board[i][j] = pg.getNextColor();
							board[i][j + 1] = pg.getNextColor();
							board[i + 1][j] = pg.getNextColor();
							board[i + 1][j + 1] = pg.getNextColor();
						}

						parent.updateDrawer();
					}
				}
			}

			if (!find) {
				break;
			}
		}
	}

	public void applyMove(int moveId, int r1, int c1, int dir) throws Exception {
		if (r1 < 0 || r1 >= N || c1 < 0 || c1 >= N) {
			throw new Exception("In move " + moveId + " (0-based), the cell chosen by your solution is outside" +
					" the board: row = " + r1 + ", col = " + c1 + ".");
		}

		int r2 = r1 + DR[dir];
		int c2 = c1 + DC[dir];

		if (r2 < 0 || r2 >= N || c2 < 0 || c2 >= N) {
			throw new Exception("In move " + moveId + " (0-based), the direction chosen by your solution" +
					" points to a cell outside the board: row = " + r2 + ", col = " + c2 + "."+dir);
		}

		synchronized (worldLock) {
			curMove++;

			exchangeR1 = r1;
			exchangeC1 = c1;
			exchangeR2 = r2;
			exchangeC2 = c2;
		}

		parent.updateDrawer();

		synchronized (worldLock) {
			int tmp = board[r1][c1];
			board[r1][c1] = board[r2][c2];
			board[r2][c2] = tmp;

			exchangeR1 = -1;
			exchangeC1 = -1;
			exchangeR2 = -1;
			exchangeC2 = -1;
		}

		parent.updateDrawer();
	}
}

class Drawer extends JFrame {
	public boolean pauseMode = false;

	final Object keyMutex = new Object();
	boolean keyPressed;

	class DrawerKeyListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			synchronized (keyMutex) {
				if (e.getKeyChar() == ' ') {
					pauseMode = !pauseMode;
				}
				keyPressed = true;
				keyMutex.notifyAll();
			}
		}
	}

	public void processPause() {
		synchronized (keyMutex) {
			if (!pauseMode) {
				return;
			}
			keyPressed = false;
			while (!keyPressed) {
				try {
					keyMutex.wait();
				} catch (InterruptedException e) {
					// do nothing
				}
			}
		}
	}

	class DrawerWindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent event) {
			System.exit(0);
		}
	}

	class DrawerPanel extends JPanel {
		final Color[] colors = new Color[] {
				new Color(255, 127, 127),
				new Color(127, 255, 127),
				new Color(127, 127, 255),
				new Color(255, 255, 127),
				new Color(127, 255, 255),
				new Color(255, 127, 255)
		};

		public void drawGrid(Graphics g, int topX, int topY) {
			g.setColor(Color.BLACK);
			for (int i = 0; i <= boardSize; i++) {
				g.drawLine(topX + i * cellSize, topY, topX + i * cellSize, topY + cellSize * boardSize);
				g.drawLine(topX, topY + i * cellSize, topX + cellSize * boardSize, topY + i * cellSize);
			}

			for (int i = 0; i < boardSize; i++) {
				for (int j = 0; j < boardSize; j++) {
					g.setColor(colors[world.board[i][j]]);
					g.fillRect(topX + j * cellSize + 1, topY + i * cellSize + 1, cellSize - 1, cellSize - 1);
				}
			}

			if (world.removeR != -1) {
				g.setColor(Color.BLACK);
				for (int dr = 0; dr < 4; dr++) {
					for (int dc = 0; dc < 4; dc++) {
						g.drawLine(topX + world.removeC * cellSize + dc * cellSize / 2,
								topY + world.removeR * cellSize + dr * cellSize / 2,
								topX + world.removeC * cellSize + (dc + 1) * cellSize / 2,
								topY + world.removeR * cellSize + (dr + 1) * cellSize / 2);

						g.drawLine(topX + world.removeC * cellSize + (dc + 1) * cellSize / 2,
								topY + world.removeR * cellSize + dr * cellSize / 2,
								topX + world.removeC * cellSize + dc * cellSize / 2,
								topY + world.removeR * cellSize + (dr + 1) * cellSize / 2);
					}
				}
			}

			if (world.exchangeR1 != -1) {
				int dr = (world.exchangeR2 - world.exchangeR1) * cellSize / 6;
				int dc = (world.exchangeC2 - world.exchangeC1) * cellSize / 6;

				int baseX1 = topX + world.exchangeC1 * cellSize + cellSize / 2;
				int baseY1 = topY + world.exchangeR1 * cellSize + cellSize / 2;
				int baseX2 = topX + world.exchangeC2 * cellSize + cellSize / 2;
				int baseY2 = topY + world.exchangeR2 * cellSize + cellSize / 2;

				g.setColor(Color.BLACK);
				g.drawLine(baseX1, baseY1, baseX2, baseY2);

				g.drawLine(baseX2, baseY2, baseX2 - dr*3/4 - dc, baseY2 + dc*3/4 - dr);
				g.drawLine(baseX2, baseY2, baseX2 + dr*3/4 - dc, baseY2 - dc*3/4 - dr);

				dr = -dr;
				dc = -dc;

				g.drawLine(baseX1, baseY1, baseX1 - dr*3/4 - dc, baseY1 + dc*3/4 - dr);
				g.drawLine(baseX1, baseY1, baseX1 + dr*3/4 - dc, baseY1 - dc*3/4 - dr);
			}
		}

		public void paint(Graphics g) {
			synchronized (world.worldLock) {
				drawGrid(g, 15, 15);

				final int horPos = 40 + boardSize * cellSize;

				g.setColor(Color.BLACK);

				g.setFont(new Font("Arial", Font.BOLD, 16));
				g.drawString("Current score:", horPos, 30);

				g.setFont(new Font("Arial", Font.BOLD, 30));
				g.drawString("" + world.score, horPos, 60);

				g.setFont(new Font("Arial", Font.BOLD, 16));
				g.drawString("Current move:", horPos, 90);

				g.setFont(new Font("Arial", Font.BOLD, 30));
				g.drawString("" + world.curMove, horPos, 120);

				g.setFont(new Font("Arial", Font.BOLD, 16));
				g.drawString("Next " + PseudorandomGenerator.KEEP / 4 + " squares to appear:",
						15, 50 + cellSize * boardSize);//when a 2*2square will be  removed , this will appear;

				ListIterator<Integer> iterator = world.pg.next.listIterator(0);
				for (int squareId = 0; squareId < PseudorandomGenerator.KEEP / 4; squareId++) {
					for (int r = 0; r < 2; r++) {
						for (int c = 0; c < 2; c++) {
							int color = iterator.next();
							int x = 15 + c * cellSize + squareId * 5 * cellSize / 2;
							int y = 70 + cellSize * (boardSize + r);

							g.setColor(Color.BLACK);
							g.drawRect(x, y, cellSize, cellSize);

							g.setColor(colors[color]);
							g.fillRect(x + 1, y + 1, cellSize - 1, cellSize - 1);
						}
					}
				}
			}
		}
	}

	World world;
	int cellSize, boardSize;
	int width, height;

	DrawerPanel panel;

	final int NEXT_TILES_CNT = PseudorandomGenerator.KEEP;

	final int EXTRA_HEIGHT = 150;
	final int EXTRA_WIDTH = 200;
	final int EXTRA_WIDTH_NEXT = 50;

	public Drawer(World world, int cellSize) {
		super();

		panel = new DrawerPanel();
		getContentPane().add(panel);

		addWindowListener(new DrawerWindowListener());
		addKeyListener(new DrawerKeyListener());

		this.world = world;
		this.cellSize = cellSize;
		this.boardSize = world.board.length;

		width = Math.max(boardSize * cellSize + EXTRA_WIDTH, NEXT_TILES_CNT * cellSize * 5 / 8 + EXTRA_WIDTH_NEXT);
		height = (boardSize + 2) * cellSize + EXTRA_HEIGHT;

		setSize(width, height);
		setTitle("TCO'14 Marathon Round 1");

		setResizable(false);
		setVisible(true);
	}
}

public class SquareRemoverVis {
	public static String execCommand = null;
	public static long seed = 1;
	public static boolean vis = true;
	public static int cellSize = 30;
	public static int delay = 100;
	public static boolean startPaused = false;
	public static long quitTime = -1;

	public static final int ANSWER_STEPS = 10000;
	public static final int ANSWER_LENGTH = 3 * ANSWER_STEPS;



	public int[] runSolution(TestCase tc, String execCmd) throws Exception {



		SquareRemover sr = new SquareRemover();



		sr.playIt(tc.colors, tc.board, tc.startSeed);

		int[] ans = sr.playIt(tc.colors, tc.board, tc.startSeed);


		return ans;
	} 



	Drawer drawer;

	public void updateDrawer() {
		if (vis) {
			drawer.processPause();
			drawer.repaint();
			try {
				if (!drawer.pauseMode) {
					Thread.sleep(delay);
				}
			} catch (Exception e) {
				// do nothing
			}
		}
	}

	public int runTest() {
		TestCase tc = new TestCase(seed);

		int[] ans;
		try {
			ans = runSolution(tc, execCommand);
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getMessage());
			return -1;
		}

		World world = new World(tc, this);

		if (vis) {
			drawer = new Drawer(world, cellSize);
			if (startPaused) {
				drawer.pauseMode = true;
			}
		}

		updateDrawer();

		world.removeSquares();

		for (int i = 0; i < ANSWER_STEPS; i++) {
			try {
				world.applyMove(i, ans[3 * i], ans[3 * i + 1], ans[3 * i + 2]);
			} catch (Exception e) {
				System.out.println("ERROR: " + e.getMessage());
				return -1;
			}

			world.removeSquares();
		}

		return world.score;
	}

	public static void main(String[] args) throws Exception {
		


		SquareRemoverVis vis = new SquareRemoverVis();
		
		
		
		try {
			int score = vis.runTest();
			System.out.println("Score  = " + score);
			if (quitTime >= 0) {
				Thread.sleep(quitTime);
				System.exit(0);
			}
		} catch (Exception e) {
			System.err.println("ERROR: Unexpected error while running your test case.");
			e.printStackTrace();
		}
	}
}

class ErrorStreamRedirector extends Thread {
    public BufferedReader reader;

    public ErrorStreamRedirector(InputStream is) {
        reader = new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
        while (true) {
            String s;
            try {
                s = reader.readLine();
            } catch (Exception e) {
                // e.printStackTrace();
                return;
            }
            if (s == null) {
                break;
            }
            System.out.println(s);
        }
    }
}

