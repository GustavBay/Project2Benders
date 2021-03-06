import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class OptimalityProblem {

	
	private GeneratorProblem gcp;
	private IloNumVar[][] p;
	private IloNumVar[] l;
	private IloCplex model;
	private IloRange[] demandConstraints;
	private IloRange[][] minProConstr;
	private IloRange[][] maxProConstr;
	private IloRange[][] RampUpConstr;
	private IloRange[][] RampDownConstr;

	public OptimalityProblem(GeneratorProblem generatorproblem, double[][] U) throws IloException {
		// Initialising variables
		this.gcp = generatorproblem;
		this.p = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.l = new IloNumVar[gcp.getT()];
		this.model = new IloCplex();
		
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				p[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
			}
			l[t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
		}
		
		// Creating Objective Function
		IloLinearNumExpr obj = model.linearNumExpr();
		
		//production costs
		for (int t=1; t <= gcp.getT(); t++) {
			obj.addTerm(gcp.getShedCost(), l[t-1]);
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				obj.addTerm(gcp.getProdCost()[g-1], p[g-1][t-1]);
			}
		}
		// minimize the objective function
		model.addMinimize(obj);
		
		//Constraint 1e
		this.demandConstraints = new IloRange[gcp.getT()];
		for (int t = 1; t <= gcp.getT(); t++) {
			IloLinearNumExpr lhs = model.linearNumExpr();
			//sum
			for (int g=1; g<=gcp.getnGenerators(); g++) {
				lhs.addTerm(1, p[g-1][t-1]);}
			lhs.addTerm(1, l[t-1]);
			demandConstraints[t-1] = model.addEq(lhs, gcp.getDemand()[t-1]);
		}
		//Constraint 1f
		this.minProConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				lhs.addTerm(1, p[g-1][t-1]);
				minProConstr[g-1][t-1]=model.addGe(lhs, gcp.getMinP()[g-1]* U[g-1][t-1]);
			}
		}
		
		//Constraint 1g
		this.maxProConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				lhs.addTerm(1, p[g-1][t-1]);
				maxProConstr[g-1][t-1]=model.addLe(lhs, gcp.getMaxP()[g-1]*U[g-1][t-1]);
			}
		}
		
		//Constraint 1h Note ramping up and down times are the same
		this.RampUpConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				if (t <= 1) {
					lhs.addTerm(1, p[g-1][t-1]);
				}else {
					lhs.addTerm(1, p[g-1][t-1]);
					lhs.addTerm(-1, p[g-1][t-2]);
				}
				RampUpConstr[g-1][t-1] = model.addLe(lhs, gcp.getRamping()[g-1]);
			}
		}
		
		//Constraint 1i Note ramping up and down times are the same
		this.RampDownConstr = new IloRange[gcp.getnGenerators()][gcp.getT()];
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				if (t <= 1) {
					lhs.addTerm(-1, p[g-1][t-1]);
				}else {
					lhs.addTerm(-1, p[g-1][t-1]);
					lhs.addTerm(1, p[g-1][t-2]);
				}
				RampDownConstr[g-1][t-1] = model.addLe(lhs, gcp.getRamping()[g-1]);
			}
		}
		
	}// Constructor
	
	
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
    
    
    // Returning Constant and linear terms for the Extended BD optimality cuts
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
    
    
    
    // Get the solution for l and p
    public double[][] getSolutionP() throws IloException{
    	double solution[][] = new double[gcp.getnGenerators()][gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		for (int g=1; g<=gcp.getnGenerators(); g++) {
    			solution[g-1][t-1] = model.getValue(p[g-1][t-1]);
    		}
    	}
    	return solution;
    }
    
    public double[] getSolutionL() throws IloException{
    	double solution[] = new double[gcp.getT()];
    	for (int t=1; t<=gcp.getT(); t++) {
    		solution[t-1] = model.getValue(l[t-1]);
    	}
    	return solution;
    }
    
    public void print() throws  IloException {
    	System.out.println("// ========= Printing solution ===========");
        
        System.out.println("\n=====Shedding===== ");
        System.out.print("[ ");
        for(int i = 1; i<=gcp.getT() ;i++){ 
        	if(model.getValue(l[i-1]) > 0) {
        		System.out.print("l_"+i+" = "+model.getValue(l[i-1])+", ");
        	}
        } System.out.print("] \n");
    	System.out.println("\n=====Generator Production===== ");
        for(int i = 1; i<= gcp.getnGenerators(); i++){
        	String str = "";
        	for(int j = 1; j<=gcp.getT() ;j++){
        		if(model.getValue(p[i-1][j-1]) > 0) {
        			str = str+gcp.getName()[i-1]+"_"+j+" = "+model.getValue(p[i-1][j-1])+" ";
        		}     
        	}   
        	if (str != "") {
            	System.out.print("[ "+str+"] \n");
            }
        }
    }
    
    public void end() {
    	model.end();
    }
	
}
