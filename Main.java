import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ilog.concert.IloException;

public class Main {

	public static void main(String[] args) throws IOException, IloException {
		// Setting Default Data
		String GeneratorFile = "generators.txt";
		String LoadFile = "loads.txt";
		int G = 31;
		int T = 24;
		double shed = 46;
		
		//Alternatively use main arguments as input
		if (args.length == 5) {
			GeneratorFile = args[2];
			LoadFile = args[1];
			G = Integer.parseInt(args[0]);
			T = Integer.parseInt(args[3]);
			shed = Double.parseDouble(args[4]);
		}
		else {
			System.out.println("No 5 arguments were supplied, thus performing default example \n"
					+ "Alternatively supply: Number of Generators, Demand File, Data Table, Number of Hours and shed cost");
			
		}
		GeneratorProblem gcp = new GeneratorProblem(G, LoadFile, GeneratorFile, T, shed);
		
		// Want to perform a direct solve, without Benders?
		System.out.println("Press Enter to continue or Enter 'y' if you want to calculate the optimal value with a generic direct model");
		//char answer = (char) System.in.read();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// Solving a direct implementation of the model, to verify correctness of the BD algorithm.
		try {
			char answer = br.readLine().charAt(0);
			if(answer == 'y') {
				DirectModel dirSol = new DirectModel(gcp);
				dirSol.solve();
				
				System.out.println("Wanna print solution? Enter 'y' or enter to continue");
				try {
					answer = br.readLine().charAt(0);
					if (answer == 'y') {
						dirSol.print();
					}
				} catch(Exception e) {}
			}
		}		catch(Exception e) {}
		
		//char answer = 'y';
		
		
		
		System.out.println("\nPress enter to continue and start the Benders Decompostion algorithm");
		System.in.read();
		
		// Bender's Decomposition
		
		
		
	}

}
