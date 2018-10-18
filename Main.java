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
		
		// The constructor for the problem will take the input files and read them
		GeneratorProblem gcp = new GeneratorProblem(G, LoadFile, GeneratorFile, T, shed);
		
		// Want to perform a direct solve, without Benders?
		System.out.println("Press Enter to continue or Enter 'y' if you want to calculate the optimal value with a generic direct model");
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
		
		
		
		
		System.out.println("\nPress enter to continue and start the Benders Decompostion algorithm");
		System.in.read();
		
		// Bender's Decomposition
		MasterProblem mp = new MasterProblem(gcp);
		boolean solved = false;
		int iter = 0;
		while (!solved) {
			mp.solve();
						
			FeasibilityProblem fsp = new FeasibilityProblem(gcp, mp.getU());
			fsp.solve();
			
			//Checking Feasibility
			if(fsp.getObjValue()>1e-6) {
				// The second stage resulted infeasible, so we will add the feasibility cut and return to top of loop
				mp.addFeasibilityCut(fsp.getDualsDemandConstraints(), fsp.getminProConstraints(), 
						fsp.getmaxProConstraints(), fsp.getRampUpConstraints(), fsp.getRampDownConstraints());
			}
			
			else {	
			// solve Optimality Subproblem checking for optimality 
			OptimalityProblem osp = new OptimalityProblem(gcp, mp.getU());
			osp.solve();
			//System.out.println("printing osp value: "+osp.getObjValue());
			//System.out.println("printing phi value: "+mp.getPhi());
			
			if( mp.getPhi()+(1e-6) >= osp.getObjValue()) {
				// If true, then we solved the problem!
				System.out.println("// ======  The Bender's Decomposition has converged in "+iter+" iterations ========");
				System.out.println(" ====> Optimal Value = "+mp.getObjValue()+"\n");
				solved = true;
				
				System.out.println("Wanna print solution? Enter 'y' or enter to continue");
				try {
					char answer = br.readLine().charAt(0);
					if (answer == 'y') {
						osp.print();
						mp.print();
					}
				} catch(Exception e) {}
				
								
				mp.end();
			}
			else {
				// Solution is not optimal and we must introduce a cut with the duals from the Optimality subproblem
				mp.addOptimalityCut(osp.getDualsDemandConstraints(), osp.getminProConstraints(), 
						osp.getmaxProConstraints(), osp.getRampUpConstraints(), osp.getRampDownConstraints());
			}
			osp.end();
			}// if feasible
			iter++;
		}//while
		
		
	}

}
