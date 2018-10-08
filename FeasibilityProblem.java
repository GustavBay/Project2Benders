import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class FeasibilityProblem {

	
	private GeneratorProblem gcp;
	private IloCplex model; // this subproblem
	private int[][] U;
	private IloNumVar p[][]; //production
	private IloNumVar l[]; //shedding
    private IloNumVar v1[][]; //positive slack
    private IloNumVar v2[][]; //negative slack
	private IloRange[] demandConstraints;
	private IloRange[][] minProConstr;
	private IloRange[][] maxProConstr;
	private IloRange[][] RampUpConstr;
	private IloRange[][] RampDownConstr;

	public FeasibilityProblem(GeneratorProblem gcp, int[][] U) throws IloException { 
		//taking the problem and the first stage solution as input
		// Initialising values
		this.gcp = gcp;
		this.U=U;
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
        //model.setOut(null);
        model.solve();
    }
    
    /**
     * @return the objective value
     */
    public double getObjValue() throws IloException{
        return model.getObjValue();
    }
    
    
    /*  I just realised the feasibility problem is totally redundant as the second stage will never be infeasible     
     *	as the second stage can always satisfy demand with the shedding variable l.
     *	So I just wasted a bit of time implementing a feasiblity problem. 
     *	The methods for returning the duals of the constraints would show bellow, but I have omitted to include them as 
     *	they are redundant.  
     */
    
    
}
