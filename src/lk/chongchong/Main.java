package lk.chongchong;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @クラス名： Main
 * @説明：
 * @作成者： サンゾーン）李
 * @作成日： 2018/09/05
 * @更新着：
 * @最後更新日：
 */
public class Main extends JFrame implements Runnable {

	private static final int FPS = 30;

	private static final int RADIUS = 20;
	private static final int DIS = 50;

	private static final int OFFSET_X = 43;
	private static final int OFFSET_Y = 41;
	private static final int OFFSET_CHESS = 60;

	private static final int FRAME_WIDTH = 570;
	private static final int FRAME_HEIGHT = OFFSET_CHESS + 570;

	private int[] dDistance = new int[]{0, 121, 241, 362, 483};
	private Image chess = new ImageIcon(getClass().getClassLoader().getResource("chessboard.jpg")).getImage();
	private Graphics graphics;

	private volatile boolean running;

	/**
	 * who
	 */
	private boolean isBlack;
	/**
	 * chessboard flags
	 * 0:white
	 * 1:black
	 * -1:empty
	 */
	private int[][] flags = new int[5][5];
	private Point clickedPoint;

	private int changeCount;
	private Object lock = new Object();


	public Main() {
		setLocation(300, 100);
		setSize(FRAME_WIDTH + 60, FRAME_HEIGHT + 60);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLayout(null);
		setVisible(true);

		JButton reStart = new JButton("restart");
		reStart.setLocation(20, 0);
		reStart.setSize(100, 20);
		reStart.addActionListener((e) -> {
			running = false;
			initData();
		});
		getContentPane().add(reStart);

		initCanvas();
		initData();

		new Thread(this).start();
	}

	public static void main(String[] args) {
		new Main();
	}

	/**
	 * init chessboard
	 */
	private void initCanvas() {
		Canvas canvas = new Canvas();
		canvas.setLocation(20, 20);
		canvas.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				if (running) {
					int clickX = e.getX() - OFFSET_X;
					int clickY = e.getY() - OFFSET_CHESS - OFFSET_Y;
					findPoint:
					for (int y = 0; y < flags.length; y++) {
						for (int x = 0; x < flags[y].length; x++) {
							int pointX = dDistance[x];
							int pointY = dDistance[y];
							double disX = Math.abs(pointX - clickX);
							double disY = Math.abs(pointY - clickY);
							if (Math.sqrt(disX * disX + disY * disY) < DIS) {
								click(x, y);
								break findPoint;
							}
						}
					}
				}
			}
		});
		getContentPane().add(canvas);
		graphics = canvas.getGraphics();
	}

	/**
	 * init data
	 */
	private void initData() {
		clickedPoint = null;
		isBlack = true;
		for (int y = 0; y < flags.length; y++) {
			for (int x = 0; x < flags[y].length; x++) {
				if (y == 0) {
					flags[x][y] = 1;
				} else if (y == 4) {
					flags[x][y] = 0;
				} else {
					flags[x][y] = -1;
				}
			}
		}
		running = true;
	}

	@Override
	public void run() {
		while (true) {
			long sTime = System.currentTimeMillis();
			//drawChess
			drawChess();
			if (running) {
				//checkGameOver
				int result = checkGameOver();
				if (result == 1) {
					JOptionPane.showMessageDialog(this, "Black is win!");
					running = false;
				} else if (result == 0) {
					JOptionPane.showMessageDialog(this, "White is win!");
					running = false;
				}
			}
			long eTime = System.currentTimeMillis();
			long sleepTime = 1000 / 30 - (eTime - sTime);
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}
	}

	private void click(int x, int y) {
		boolean selectedPoint = isBlack && flags[x][y] == 1 || !isBlack && flags[x][y] == 0;
		if (selectedPoint) {
			//select point
			clickedPoint = new Point(x, y);
			return;
		}

		if (clickedPoint != null) {
			int xR = x - clickedPoint.x;
			int yR = y - clickedPoint.y;

			//There are two ways to move
			//One: Move horizontally or vertically, that is, two points have the same X or Y.
			//Two: Diagonal movement, the first point has 8 directions, and the two points are diagonal

			/* hasSameXorY  xR is 0 or yR is 0
			 ?  ?  ?     ? [?] ?
			[?] ? [?]    ?  ?  ?
			 ?  ?  ?     ? [?] ?
			*/
			boolean hasSameXorY = xR * yR == 0;

			/* has8direction  X: the first point has 8 directions
			 [X] [+] [X]
			 [+]  X  [+]
			 [X] [+] [X]
			*/
			boolean has8direction = (clickedPoint.x + clickedPoint.y) % 2 == 0;

			/* isDiagonal [X]:the two points are diagonal
			[X]  +   X   +  [X]
			 +  [X]  +  [X]  +
			 X   +  [X]  +   X
			 +  [X]  +  [X]  +
			[X]  +   X   +  [X]
			*/
			boolean isDiagonal = Math.abs(xR) == Math.abs(yR);
			boolean canMove = hasSameXorY || has8direction && isDiagonal;
			if (canMove) {
				int count = 0;
				if (xR != 0) {
					count = Math.abs(xR);
					xR /= count;
				}
				if (yR != 0) {
					count = Math.abs(yR);
					yR /= count;
				}
				int tempDx = xR;
				int tempDy = yR;
				for (int i = 0; i < count; i++) {
					if (flags[clickedPoint.x + tempDx][clickedPoint.y + tempDy] != -1) {
						//have barrier
						return;
					}
					tempDx += xR;
					tempDy += yR;
				}
				flags[clickedPoint.x][clickedPoint.y] = -1;
				flags[x][y] = isBlack ? 1 : 0;

				clickedPoint = null;
				synchronized (lock) {
					changeCount = -1;
					checkAction(x, y);
				}
				isBlack = !isBlack;
			}
		}
	}

	/**
	 * check eat
	 *
	 * @param x
	 * @param y
	 */
	private void checkAction(int x, int y) {
		changeCount++;
		checkFour(x, y);
		checkThree(x, y);
		checkTwo(x, y);
		checkOne(x, y);
	}

	/**
	 * Play four
	 *
	 * @param x
	 * @param y
	 */
	private void checkFour(int x, int y) {
		int flag = isBlack ? 1 : 0;
		/*
			[X] [+] [X] [+] [X]
			[+] [X]  +   X   +
			[X]  +  [X]  +   X
			[+]  X   +  [X]  +
			[X]  +   X   +  [X]
		*/
		if (x == 0 || x == 4 || y == 0 || y == 4) {
			if (x == 0 || x == 4) {
				int startX = 1;
				if (x == 4) {
					startX = 0;
				}
				boolean isOK = true;
				for (int i = startX; i < startX + 4; i++) {
					if (flags[i][y] == -1 || flags[i][y] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = startX; i < startX + 4; i++) {
						if (flags[i][y] != flag) {
							flags[i][y] = flag;
							checkAction(i, y);
						}
					}
				}
			}
			if (y == 0 || y == 4) {
				int startY = 1;
				if (y == 4) {
					startY = 0;
				}
				boolean isOK = true;
				for (int i = startY; i < startY + 4; i++) {
					if (flags[x][i] == -1 || flags[x][i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = startY; i < startY + 4; i++) {
						if (flags[x][i] != flag) {
							flags[x][i] = flag;
							checkAction(x, i);
						}
					}
				}
			}
			if (x == 0 && y == 0 || x == 4 && y == 4) {
				int start = 1;
				if (x == 4) {
					start = 0;
				}
				boolean isOK = true;
				for (int i = start; i < start + 4; i++) {
					if (flags[i][i] == -1 || flags[i][i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = start; i < start + 4; i++) {
						if (flags[i][i] != flag) {
							flags[i][i] = flag;
							checkAction(i, i);
						}
					}
				}
			}

			if (x == 0 && y == 4 || x == 4 && y == 0) {
				int start = 1;
				if (x == 4) {
					start = 0;
				}
				boolean isOK = true;
				for (int i = start; i < start + 4; i++) {
					if (flags[i][4 - i] == -1 || flags[i][4 - i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = start; i < start + 4; i++) {
						if (flags[i][4 - i] != flag) {
							flags[i][4 - i] = flag;
							checkAction(i, 4 - i);
						}
					}
				}
			}
		}
	}

	/**
	 * Play three
	 *
	 * @param x
	 * @param y
	 */
	private void checkThree(int x, int y) {
		int flag = isBlack ? 1 : 0;

		if (x == 0 || x == 4 || y == 0 || y == 4) {
			if ((x == 0 || x == 4) && flags[0][y] == flags[4][y]) {
				boolean isOK = true;
				for (int i = 1; i < 4; i++) {
					if (flags[i][y] == -1 || flags[i][y] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = 1; i < 4; i++) {
						flags[i][y] = flag;
						checkAction(i, y);
					}
				}
			}
			if ((y == 0 || y == 4) && flags[x][0] == flags[x][4]) {
				boolean isOK = true;
				for (int i = 1; i < 4; i++) {
					if (flags[x][i] == -1 || flags[x][i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = 1; i < 4; i++) {
						flags[x][i] = flag;
						checkAction(x, i);
					}
				}
			}
			if ((x == 0 && y == 0 || x == 4 && y == 4) && flags[0][0] == flags[4][4]) {
				boolean isOK = true;
				for (int i = 1; i < 4; i++) {
					if (flags[i][i] == -1 || flags[i][i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = 1; i < 4; i++) {
						flags[i][i] = flag;
						checkAction(i, i);
					}
				}
			}

			if ((x == 0 && y == 4 || x == 4 && y == 0) && flags[4][0] == flags[0][4]) {
				boolean isOK = true;
				for (int i = 1; i < 4; i++) {
					if (flags[4 - i][i] == -1 || flags[4 - i][i] == flag) {
						isOK = false;
						break;
					}
				}
				if (isOK) {
					for (int i = 1; i < 4; i++) {
						flags[4 - i][i] = flag;
						checkAction(4 - i, i);
					}
				}
			}
		}

	}

	/**
	 * Play two
	 *
	 * @param x
	 * @param y
	 */
	private void checkTwo(int x, int y) {
		int flag = isBlack ? 1 : 0;

		if (!(x == 0 && (y == 0 || y == 4) || x == 4 && (y == 0 || y == 4))) {
			if (x - 1 >= 0 && x + 1 <= 4) {
				if (flags[x - 1][y] == flags[x + 1][y] && flags[x - 1][y] != -1 && flags[x - 1][y] != flag) {
					flags[x - 1][y] = flag;
					checkAction(x - 1, y);
					flags[x + 1][y] = flag;
					checkAction(x + 1, y);
				}
			}

			if (y - 1 >= 0 && y + 1 <= 4) {
				if (flags[x][y - 1] == flags[x][y + 1] && flags[x][y - 1] != -1 && flags[x][y - 1] != flag) {
					flags[x][y - 1] = flag;
					checkAction(x, y - 1);
					flags[x][y + 1] = flag;
					checkAction(x, y + 1);
				}
			}

			//8
			if ((x + y) % 2 == 0) {
				if (x - 1 >= 0 && x + 1 <= 4 && y - 1 >= 0 && y + 1 <= 4) {
					if (flags[x - 1][y - 1] == flags[x + 1][y + 1] && flags[x - 1][y - 1] != -1 && flags[x - 1][y - 1] != flag) {
						flags[x - 1][y - 1] = flag;
						checkAction(x - 1, y - 1);
						flags[x + 1][y + 1] = flag;
						checkAction(x + 1, y + 1);
					}

					if (flags[x - 1][y + 1] == flags[x + 1][y - 1] && flags[x - 1][y + 1] != -1 && flags[x - 1][y + 1] != flag) {
						flags[x - 1][y + 1] = flag;
						checkAction(x - 1, y + 1);
						flags[x + 1][y - 1] = flag;
						checkAction(x + 1, y - 1);
					}
				}
			}
		}
	}

	/**
	 * Play one
	 *
	 * @param x
	 * @param y
	 */
	private void checkOne(int x, int y) {
		int flag = isBlack ? 1 : 0;

		for (int i = -2; i < 3; i += 2) {
			for (int j = -2; j < 3; j += 2) {
				if (x - i >= 0 && x - i <= 4 && y - j >= 0 && y - j <= 4) {
					if (i == 0 && j == 0) {
						continue;
					}
					if (i == 0 || j == 0 || (x + y) % 2 == 0) {
						int ff = flags[x - i][y - j];
						int f = flags[x - i / 2][y - j / 2];
						if (ff != -1 && ff == flag && f != -1 && f != flag) {
							flags[x - i / 2][y - j / 2] = flag;
							checkAction(x - i / 2, y - j / 2);
						}
					}
				}
			}
		}
	}


	/**
	 * draw result
	 */
	private void drawChess() {
		Image image = this.createImage(FRAME_WIDTH, FRAME_HEIGHT);
		Graphics g = image.getGraphics();
		//draw background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
		//draw flag
		g.setColor(isBlack ? Color.BLACK : Color.WHITE);
		g.fillOval(0, 0, 2 * RADIUS, 2 * RADIUS);
		g.setColor(Color.BLACK);
		g.drawOval(0, 0, 2 * RADIUS, 2 * RADIUS);
		//draw message
		synchronized (lock) {
			if (changeCount > 7) {
				g.drawString("perfect!", 500, 20);
			} else if (changeCount > 5) {
				g.drawString("amazing!", 500, 20);
			} else if (changeCount > 3) {
				g.drawString("good!", 500, 20);
			} else {
				g.drawString("", 500, 20);
			}
		}


		//draw chess
		g.drawImage(chess, 0, OFFSET_CHESS, this);
		//draw flags
		for (int y = 0; y < flags.length; y++) {
			for (int x = 0; x < flags[y].length; x++) {
				if (flags[x][y] != -1) {

					int pointX = dDistance[x];
					int pointY = dDistance[y];

					if (clickedPoint != null && x == clickedPoint.x && y == clickedPoint.y) {
						g.setColor(flags[x][y] == 1 ? Color.gray : Color.gray);
					} else {
						g.setColor(flags[x][y] == 1 ? Color.BLACK : Color.WHITE);
					}
					g.fillOval(pointX + OFFSET_X - RADIUS, pointY + OFFSET_CHESS + OFFSET_Y - RADIUS, 2 * RADIUS, 2 * RADIUS);
					g.setColor(Color.BLACK);
					g.drawOval(pointX + OFFSET_X - RADIUS, pointY + OFFSET_CHESS + OFFSET_Y - RADIUS, 2 * RADIUS, 2 * RADIUS);
				}
			}
		}
		graphics.drawImage(image, 0, 0, this);
	}

	/**
	 * check game over
	 *
	 * @return
	 */
	private int checkGameOver() {
		boolean findB = false;
		boolean findW = false;
		for (int y = 0; y < flags.length; y++) {
			for (int x = 0; x < flags[y].length; x++) {
				if (flags[x][y] == 1) {
					findB = true;
				} else if (flags[x][y] == 0) {
					findW = true;
				}
				if (findB && findW) {
					return -1;
				}
			}
		}
		return findB ? 1 : 0;
	}
}