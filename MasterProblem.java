import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;




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
	
	
	// Utility Methods
	/**
	 * @return the phi
	 * @throws IloException 
	 * @throws  
	 */
	public double getPhi() throws IloException {
		return model.getValue(phi);
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
    }
    
	
    public void print() throws IloException {
        
        System.out.println("\n=====Generator On Status===== ");
        for(int i = 1; i<= gcp.getnGenerators(); i++){
        	String str = ""; 
            for(int j = 1; j<=gcp.getT() ;j++){
            	if (model.getValue(u[i-1][j-1]) > 0 ) {
            		str = str+gcp.getName()[i-1]+"_"+j+" = On  ";
            	}
            }
            if (str != "") {
            	System.out.print("[ "+str+"] \n");
            }
        }
        //System.out.println("Production Cost Phi = "+model.getValue(phi));
        System.out.println(" \n ===> Optimal objective value "+model.getObjValue()+"\n");
        
    }
    
    public void end(){
        model.end();
    }
    
    public double getObjValue() throws IloException {
    	return model.getObjValue();
    }
	
    // Set Optimality Cuts (feasibility cuts are omitted as they are redundant in this case)
    //  u^j(b-Gx) - phi <= 0
    public void addOptimalityCut(double[] demandDuals, double[][] minProDuals, double[][] maxProDuals,
    							 double[][] RampUpDuals, double[][] RampDownDuals) throws IloException {
    	IloLinearNumExpr lhs = model.linearNumExpr();
    	// adding constants
    	double constant = 0;
    	for (int t=1; t<=gcp.getT(); t++) {
    		constant += gcp.getDemand()[t-1]*demandDuals[t-1];
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			constant += gcp.getRamping()[g-1]*RampDownDuals[g-1][t-1];
    			constant += gcp.getRamping()[g-1]*RampUpDuals[g-1][t-1];
    		}
    	}
    	lhs.setConstant(constant);
    	// adding u-terms
    	for(int t=1; t<=gcp.getT();t++) {
    		for (int g=1; g<=gcp.getnGenerators();g++) {
    			lhs.addTerm(minProDuals[g-1][t-1]*gcp.getMinP()[g-1], u[g-1][t-1]);
    			lhs.addTerm(maxProDuals[g-1][t-1]*gcp.getMaxP()[g-1], u[g-1][t-1]);
    		}
    	}
    	lhs.addTerm(-1, phi);
    	//IloRange cut = 
    			model.addLe(lhs, 0);
        //System.out.println("Adding cut "+cut.toString());  	
    }
    
 // Set Feasibility Cuts 
    //  u^j(b-Gx) - phi <= 0
    public void addFeasibilityCut(double[] demandDuals, double[][] minProDuals, double[][] maxProDuals,
    							 double[][] RampUpDuals, double[][] RampDownDuals) throws IloException {
    	IloLinearNumExpr lhs = model.linearNumExpr();
    	// adding constants
    	double constant = 0;
    	for (int t=1; t<=gcp.getT(); t++) {
    		constant += gcp.getDemand()[t-1]*demandDuals[t-1];
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			constant += gcp.getRamping()[g-1]*RampDownDuals[g-1][t-1];
    			constant += gcp.getRamping()[g-1]*RampUpDuals[g-1][t-1];
    		}
    	}
    	lhs.setConstant(constant);
    	// adding u-terms
    	for(int t=1; t<=gcp.getT();t++) {
    		for (int g=1; g<=gcp.getnGenerators();g++) {
    			lhs.addTerm(minProDuals[g-1][t-1]*gcp.getMinP()[g-1], u[g-1][t-1]);
    			lhs.addTerm(maxProDuals[g-1][t-1]*gcp.getMaxP()[g-1], u[g-1][t-1]);
    		}
    	}
    	//IloRange cut = 
    			model.addLe(lhs, 0);
        //System.out.println("Adding cut "+cut.toString());    	
    }
    
    
	
}
