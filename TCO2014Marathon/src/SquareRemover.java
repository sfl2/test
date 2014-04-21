import java.util.Arrays;
import java.util.Random;


public class SquareRemover {

	int move;
	final static int MAXMOVE = 10000;
	long leftmost;
	int color;
	int[] ans = new int[30000];

	int N;

	final static int[] dx = {-1,0,1,0};
	final static int[] dy = {0,1,0,-1};

	int getTileFromBuffer(){
		leftmost = (leftmost*48271)%2147483647;
		return (int) (leftmost%color);
	}

	int[] playIt(int colors,String[] board,int startSeed){

		leftmost = startSeed;
		N = board.length;
		Random random = new Random();

		for(int i = 0; i < ans.length; i+=3){
			ans[i] = 0;
			ans[i+1] = 1;
			ans[i+2] = 2;
			
			
			ans[i] = random.nextInt(N);
			ans[i+1] = random.nextInt(N);

			int x = ans[i];
			int y = ans[i+1];
			while(true){
				int dr =random.nextInt(4);
				if(0 <= x+dx[dr] && x+dx[dr] < N ){
					if(0 <= y+dy[dr] && y+dy[dr] < N ){
						ans[i+2] = dr;
						break;
					}
				}
			}
		}

		return ans;
	}

}
