package objects;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Techplot helper functions
 * @author port091
 * @author rodr144
 */

public class TecplotNode {
	
	private class Node {
		int[] vertices = new int[8];
		
		public Node(){
		}
		
		@Override
		public String toString(){
			String output = Integer.toString(vertices[0]) +
					" " + Integer.toString(vertices[1]) +
					" " + Integer.toString(vertices[3]) +
					" " + Integer.toString(vertices[2]) +
					" " + Integer.toString(vertices[4]) +
					" " + Integer.toString(vertices[5]) +
					" " + Integer.toString(vertices[7]) +
					" " + Integer.toString(vertices[6]);
			return output;
		}
		
	}
	
	public Node[] nodes;
	
	public TecplotNode(int iDim, int jDim, int kDim){
		int flatDim = iDim*jDim*kDim;
		nodes = new Node[flatDim];
		for(int n=0; n<flatDim;  n++){
			nodes[n] = new Node();
			if(n%iDim != 0) getEastFace(n, iDim, jDim, kDim);
			if((n/iDim)%jDim != 0) getNorthFace(n, iDim, jDim, kDim);
			if((n/(iDim*jDim))%kDim != 0) getTopFace(n, iDim, jDim, kDim);
			for(int m=0; m<8; m++){
				if(nodes[n].vertices[m] == 0) nodes[n].vertices[m] = n*8+m+1;
			}
		}
		
	}
	
	private void getEastFace(int id, int iDim, int jDim, int kDim){
		nodes[id].vertices[0] = nodes[id - 1].vertices[1];
		nodes[id].vertices[2] = nodes[id - 1].vertices[3];
		nodes[id].vertices[4] = nodes[id - 1].vertices[5];
		nodes[id].vertices[6] = nodes[id - 1].vertices[7];
	}
	
	private void getNorthFace(int id, int iDim, int jDim, int kDim){
		nodes[id].vertices[0] = nodes[id - iDim].vertices[2];
		nodes[id].vertices[1] = nodes[id - iDim].vertices[3];
		nodes[id].vertices[4] = nodes[id - iDim].vertices[6];
		nodes[id].vertices[5] = nodes[id - iDim].vertices[7];
	}

	private void getTopFace(int id, int iDim, int jDim, int kDim){
		nodes[id].vertices[0] = nodes[id - iDim*jDim].vertices[4];
		nodes[id].vertices[1] = nodes[id - iDim*jDim].vertices[5];
		nodes[id].vertices[2] = nodes[id - iDim*jDim].vertices[6];
		nodes[id].vertices[3] = nodes[id - iDim*jDim].vertices[7];
	}
	
	public static String getStringOutput(int iDim, int jDim, int kDim){
		TecplotNode t = new TecplotNode(iDim, jDim, kDim);
		StringBuilder output = new StringBuilder();
		for(int i=0; i<iDim*jDim*kDim; i++){
			output.append(t.nodes[i].toString() + "\n");
		}
		return output.toString();
	}
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException{
		int iDim = 2;
		int jDim = 1;
		int kDim = 2;
		TecplotNode t = new TecplotNode(iDim, jDim, kDim);
		PrintWriter writer = new PrintWriter("TecplotTest.txt", "UTF-8");
		for(int i=0; i<iDim*jDim*kDim; i++){
			writer.println(t.nodes[i].toString());
		}
		writer.close();
	}
	
}
