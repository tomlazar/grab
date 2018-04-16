import java.net.*;
import java.awt.*;

public class GameGroup extends Thread {

	GameClientThread arr[];
	final int SIZE=2;

	int grid[][];  //map of the board
	public static final int GWD=12; // width
	public static final int GHT=10; // and height of board
	Player red, blue;  //The two players


	GameGroup ( Socket s ) {
		arr = new GameClientThread[SIZE];
		addClient( s );
	}

	public void addClient( Socket s ) {
		int x;

		for( x=0; x<SIZE; x++)
			if( arr[x] == null || !arr[x].isAlive() ) {
				arr[x] = new GameClientThread(s,this);
				arr[x].start();
				return ;
				}
	}

	public void run() {
		Point p;

		System.out.println("GameGroup begun");
		//Get a random starting board
		String board = fillGrid();

		//Position the two players - Note, we never use	the colors here
		p = emptySpot();
		blue = new Player(p.x, p.y, (int)(4*Math.random()), null);

		// We also need to mark each player's spot in the grid, so we'll
		// know it's not empty
		grid[p.x][p.y] = 3;
		p = emptySpot();
		red = new Player(p.x, p.y, (int)(4*Math.random()), null);
		grid[p.x][p.y] = 3;

		//Send each player the config.
		output("start,"+board);
		//and player info (including which they are)
		output("blue,"+blue.x+","+blue.y+","+blue.dir);
		output("red,"+red.x+","+red.y+","+red.dir);
		// We don't use output() here, because we need to send
		// different messages to each player
		arr[0].message("who,blue");
		arr[1].message("who,red");
	}

	public String fillGrid()
	{
		// fill in the board at random and return
		// a String representing the board in row-major order
		// Coords are used like screen coords - 0,0 in top-left,
		// first coord is to right, second is down.
		int x,y,i;
		Point p;

		grid = new int[GWD][GHT];
		// Clear grid
		for (x = 0; x < GWD; x++)
		 for (y = 0; y < GHT; y++)
			grid[x][y] = 0;
		// Place blocks
		for (i = 0; i < 40; i++)
		{
			p = emptySpot();
			grid[p.x][p.y] = 1;
		}
		// Place money
		for (i = 0; i < 8; i++)
		{
			p = emptySpot();
			grid[p.x][p.y] = 2;
		}
		//Now, make the string
		StringBuffer sb = new StringBuffer(GHT*GWD);
		for (y = 0; y < GHT; y++)
		 for (x = 0; x < GWD; x++)
			sb.append(grid[x][y]);
		return new String(sb);
	}

	public Point emptySpot()
	{
		int x, y;
		// Find an empty square in the grid
		do
		{
			x = (int)(GWD*Math.random());
			y = (int)(GHT*Math.random());
		} while (grid[x][y] != 0);
		return new Point(x,y);
	}

	public synchronized void processMessage(String msg)
	{
		Player p;

		//System.out.println("pM got:"+msg);

		//Chop up the message, adjust the state, and tell the clients
		String[] words = msg.split(",");
		String cmd = words[0];

		// get the player name and find the correct
		// Player object
		// NOTE: This depends on all of the messages having the
		//   same "command,name" structure.
		String pname = words[1];
		if (pname.equals("blue"))
			p = blue;
		else
			p = red;

		if (cmd.equals("turnleft"))
		{
			p.turnLeft();
			output(pname+","+p.x+","+p.y+","+p.dir);
		}
		else if (cmd.equals("turnright"))
		{
			p.turnRight();
			output(pname+","+p.x+","+p.y+","+p.dir);
		}
		else if (cmd.equals("step"))
		{
			int newx=-1, newy=-1;	//set to illegal subscripts in case the
									//logic below ever fails (at least we'll
									// get a message).

			//Compute new location
			switch(p.dir)
			{
				case Player.UP: newx = p.x; newy = p.y-1;
								if (newy < 0) return;
					break;
				case Player.RIGHT: newx = p.x+1; newy = p.y;
								if (newx >= GameGroup.GWD) return;
					break;
				case Player.DOWN: newx = p.x; newy = p.y+1;
								if (newy >= GameGroup.GHT) return;
					break;
				case Player.LEFT: newx = p.x-1; newy = p.y;
								if (newx < 0) return;
					break;
			}
			if (grid[newx][newy] != 0)
				return;
			// Clear mark in grid first
			grid[p.x][p.y] = 0;
			p.x = newx; p.y = newy;
			// Then, mark the new spot
			grid[p.x][p.y] = 3;
			output(pname+","+p.x+","+p.y+","+p.dir);
		}
	}

	public void output(String str) {
	// Send a message to each client
		int x;

		for(x=0;x<SIZE;x++)
			if(arr[x] != null)
				arr[x].message(str);
	}

	public boolean full() {
	// Check if we have all our players
		int x;

		for(x=0;x<SIZE;x++)
			if( arr[x] == null )
				return false;
		return true;
	}
}
