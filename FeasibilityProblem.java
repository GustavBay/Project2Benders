import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class FeasibilityProblem {

	
	private GeneratorProblem gcp;
	private IloCplex model; // this subproblem
	private IloNumVar p[][]; //production
	private IloNumVar l[]; //shedding
    private IloNumVar v1[][]; //positive slack
    private IloNumVar v2[][]; //negative slack
	private IloRange[] demandConstraints;
	private IloRange[][] minProConstr;
	private IloRange[][] maxProConstr;
	private IloRange[][] RampUpConstr;
	private IloRange[][] RampDownConstr;

	public FeasibilityProblem(GeneratorProblem gcp, double[][] U) throws IloException { 
		//taking the problem and the first stage solution as input
		// Initialising values
		this.gcp = gcp;
		this.p = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.l = new IloNumVar[gcp.getT()];
		this.model = new IloCplex(); 
		this.v1 = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.v2 = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				p[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
				v1[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
				v2[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
			}
			l[t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
		}
		
		//minimising over slack variables.
		IloLinearNumExpr obj = model.linearNumExpr();
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				obj.addTerm(1, v1[g-1][t-1]);
				obj.addTerm(1, v2[g-1][t-1]);
			}
		}
        model.addMinimize(obj);
        
        // Constraints
        
        //Constraint 1e (demand constraint)
        this.demandConstraints = new IloRange[gcp.getT()];
        for (int t=1; t <= gcp.getT(); t++) {
        	IloLinearNumExpr lhs = model.linearNumExpr();
        	//sum
        	for (int g=1; g<=gcp.getnGenerators(); g++) {
        		lhs.addTerm(1, p[g-1][t-1]);
        	}
        	lhs.addTerm(1, l[t-1]);
        	demandConstraints[t-1] = model.addEq(lhs, gcp.getDemand()[t-1]);
        }
        
        // Constraint 1f (minimum production)
        this.minProConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
        for(int t=1; t<=gcp.getT(); t++) {
        	for (int g=1; g<=gcp.getnGenerators(); g++) {
        		IloLinearNumExpr lhs = model.linearNumExpr();
        		lhs.addTerm(1, p[g-1][t-1]);
        		lhs.addTerm(1, v1[g-1][t-1]);
        		lhs.addTerm(-1, v2[g-1][t-1]);
        		minProConstr[g-1][t-1]=model.addGe(lhs, gcp.getMinP()[g-1]*U[g-1][t-1]);
        	}
        }
        
        // Constraint 1g (maximum production)
        this.maxProConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
        for(int t=1; t<=gcp.getT(); t++) {
        	for (int g=1; g<=gcp.getnGenerators(); g++) {
        		IloLinearNumExpr lhs = model.linearNumExpr();
        		lhs.addTerm(1, p[g-1][t-1]);
        		lhs.addTerm(1, v1[g-1][t-1]);
        		lhs.addTerm(-1, v2[g-1][t-1]);
        		maxProConstr[g-1][t-1] = model.addLe(lhs, gcp.getMaxP()[g-1]*U[g-1][t-1]);
        	}
        }
        
        // Constraint 1h (Ramp up constraint)
        this.RampUpConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
        for(int t=1; t<=gcp.getT(); t++) {
        	for (int g=1; g<=gcp.getnGenerators(); g++) {
        		IloLinearNumExpr lhs = model.linearNumExpr();
        		if (t<=1) {
        			lhs.addTerm(1, p[g-1][t-1]);
        		}
        		else {
	        		lhs.addTerm(1, p[g-1][t-1]);
	        		lhs.addTerm(-1, p[g-1][t-2]);
	        	}
        		RampUpConstr[g-1][t-1] = model.addLe(lhs, gcp.getRamping()[g-1]);
        	}
        }
        
     // Constraint 1i (Ramp Down constraint)
        this.RampDownConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
        for(int t=1; t<=gcp.getT(); t++) {
        	for (int g=1; g<=gcp.getnGenerators(); g++) {
        		IloLinearNumExpr lhs = model.linearNumExpr();
        		if (t<=1) {
        			lhs.addTerm(-1, p[g-1][t-1]);
        		}
        		else {
	        		lhs.addTerm(-1, p[g-1][t-1]);
	        		lhs.addTerm(1, p[g-1][t-2]);
	        	}
        		RampDownConstr[g-1][t-1] = model.addLe(lhs, gcp.getRamping()[g-1]);
        	}
        }
        
		
	}// Closing constructor
	
    /**
     * Solves the subproblem
     */
    public void solve() throws IloException{
        model.setOut(null);
        model.solve();
    }
    
    /**
     * @return the objective value
     */
    public double getObjValue() throws IloException{
        return model.getObjValue();
    }
    
    
 // get duals Duals
    public double[] getDualsDemandConstraints() throws IloException{
        double duals[] = new double[gcp.getT()];
        for(int t = 1; t <= gcp.getT(); t++){
            duals[t-1] = model.getDual(demandConstraints[t-1]);
        }
        return duals;
    }
    
    public double[][] getminProConstraints() throws IloException{
    	double duals[][] = new double[gcp.getnGenerators()][gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			duals[g-1][t-1] = model.getDual(minProConstr[g-1][t-1]);
    		}
    	}    	
    	return duals;
    }
    
    public double[][] getmaxProConstraints() throws IloException{
    	double duals[][] = new double[gcp.getnGenerators()][gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			duals[g-1][t-1] = model.getDual(maxProConstr[g-1][t-1]);
    		}
    	}    	
    	return duals;
    }
    
    public double[][] getRampUpConstraints() throws IloException{
    	double duals[][] = new double[gcp.getnGenerators()][gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			duals[g-1][t-1] = model.getDual(RampUpConstr[g-1][t-1]);
    		}
    	}    	
    	return duals;
    }
    
    public double[][] getRampDownConstraints() throws IloException{
    	double duals[][] = new double[gcp.getnGenerators()][gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			duals[g-1][t-1] = model.getDual(RampDownConstr[g-1][t-1]);
    		}
    	}    	
    	return duals;
    }
    
    
    //Returning Constant and linear terms for the Extended BD feasibility cuts
    public double getConstantTerm() throws IloException{
    	double constant = 0;
    	for (int t=1; t<=gcp.getT(); t++) {
    		constant += gcp.getDemand()[t-1]*model.getDual(demandConstraints[t-1]);
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			constant += gcp.getRamping()[g-1]*model.getDual(RampUpConstr[g-1][t-1]);
    			constant += gcp.getRamping()[g-1]*model.getDual(RampDownConstr[g-1][t-1]);
    		}
    	}
    	return constant;
    }
    
    public IloLinearNumExpr getLinearTerm(IloNumVar u[][]) throws IloException{
    	IloLinearNumExpr lhs = model.linearNumExpr();
    	for(int t=1; t<=gcp.getT();t++) {
    		for (int g=1; g<=gcp.getnGenerators();g++) {
    			lhs.addTerm(model.getDual(minProConstr[g-1][t-1])*gcp.getMinP()[g-1], u[g-1][t-1]);
    			lhs.addTerm(model.getDual(maxProConstr[g-1][t-1])*gcp.getMaxP()[g-1], u[g-1][t-1]);
    		}
    	}
    	return lhs;
    }
    
    
}
