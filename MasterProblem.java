import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;




public class MasterProblem {
	

	private final GeneratorProblem gcp;
	private IloCplex model;
	private IloNumVar c[][]; //startup cost
	private IloNumVar u[][]; // on/off status
	private IloNumVar phi; 
	
	public MasterProblem(GeneratorProblem gcp) throws IloException {
		this.gcp = gcp;
		this.model = new IloCplex();
		this.c = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.u = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				c[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
				u[g-1][t-1] = model.intVar(0, 1);
			}
		}
		// As the Production costs cannot be negative, we can bind phi at zero
		phi = model.numVar(0, Double.POSITIVE_INFINITY);
		
		// Creating Objective function
		IloLinearNumExpr obj = model.linearNumExpr();
		//online and startup costs
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				obj.addTerm(1, c[g-1][t-1]);
				obj.addTerm(gcp.getOnCost()[g-1], u[g-1][t-1]);
			}
		}
		obj.addTerm(1, phi);
		// minimize the objective function
        model.addMinimize(obj);
        
     // Create constraints
        // Constraint 1b
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				IloLinearNumExpr rhs = model.linearNumExpr();
				lhs.addTerm(1, c[g-1][t-1]);
				if( t == 1) { // t_0 is zero
					rhs.addTerm(gcp.getStartGenCost()[g-1], u[g-1][t-1]);
				}
				else {
				rhs.addTerm(gcp.getStartGenCost()[g-1], u[g-1][t-1]);
				rhs.addTerm(-gcp.getStartGenCost()[g-1], u[g-1][t-2]);
				}
				model.addGe(lhs, rhs);
			}
		}
		//Constraint 1c
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				//sum
				for (int j = t; j <= TU(t, g); j++) {
					if (j <= 1) {
						lhs.addTerm(1, u[g-1][t-1]);
						lhs.addTerm(-1, u[g-1][j-1]);
					}
					else {
						lhs.addTerm(1, u[g-1][t-1]);
						lhs.addTerm(-1, u[g-1][j-1]);
						lhs.addTerm(1, u[g-1][j-2]);
					}					
				}
				model.addGe(lhs, 0);
			}
		}
		//Constraint 1d
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				//sum
				for (int j = t; j <= TD(t, g); j++) {
					if (j <= 1) {
						lhs.addTerm(1, u[g-1][t-1]);
						lhs.addTerm(-1, u[g-1][j-1]);
					}
					else {
						lhs.addTerm(-1, u[g-1][t-1]);
						lhs.addTerm(1, u[g-1][j-1]);
						lhs.addTerm(-1, u[g-1][j-2]);
					}					
				}
				//moving the summation of ones to the rhs. Sum of ones from t to TD is TD-t+1
				model.addGe(lhs, -(TD(t,g)-t+1));
			}
		}
		
	}// Constructor
	
	// Feasibility Cuts are omitted as the shedding variable l will always make the second stage feasible. 
	
	//Generate Optimality Cuts
	
	
	
	
	// Utility Methods
	/**
	 * @return the phi
	 */
	public IloNumVar getPhi() {
		return phi;
	}
	
	/**
	 * @return the solution from first stage problem; i.e. what generators are operational
	 */
	public int[][] getU() throws IloException{
		int[][] U = new int[gcp.getnGenerators()][gcp.getT()];
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				U[g-1][t-1] = (int) model.getValue(u[g-1][t-1]);
			}
		}
		return U;
	}
	
	// TU and TD methods
	private int TU (int t, int gen) {
		return Math.min(t+gcp.getMinT()[gen-1]-1,gcp.getT());
	}
	private int TD (int t, int gen) {
		return Math.min(t+gcp.getMaxT()[gen-1]-1,gcp.getT());
	}
	
	
    public void solve() throws IloException{
        model.setOut(null);
        model.solve();
        System.out.println(" \n ===> Optimal objective value "+model.getObjValue()+"\n");
    }
    
	
    public void print() throws IloException {
    	System.out.println("Optimal solution ");
        
        System.out.println("\n=====Generator On Status===== ");
        for(int i = 1; i<= gcp.getnGenerators(); i++){
        	String str = ""; 
            for(int j = 1; j<=gcp.getT() ;j++){
            	if (model.getValue(u[i-1][j-1]) != 0 ) {
            		str = str+gcp.getName()[i-1]+"_"+j+" = On  ";
            	}
            }
            if (str != "") {
            	System.out.print("[ "+str+"] \n");
            }
        }
        System.out.println("Production Cost Phi = "+model.getValue(phi));
        
    }
    
    public void end(){
        model.end();
    }
	
	
}
