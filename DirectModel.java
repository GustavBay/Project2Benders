import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class DirectModel {
	
	
	private final GeneratorProblem gcp;
	private IloCplex model;
	private IloNumVar c[][]; //startup cost
	private IloNumVar u[][]; // on/off status
	private IloNumVar p[][]; //production
	private IloNumVar l[]; //shedding
	

	public DirectModel(GeneratorProblem gcp) throws IloException {
		// Initalising variables 
		this.gcp = gcp;
		this.model = new IloCplex();
		this.c = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.u = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.p = new IloNumVar[gcp.getnGenerators()][gcp.getT()];
		this.l = new IloNumVar[gcp.getT()];
		
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				c[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
				u[g-1][t-1] = model.intVar(0, 1);
				p[g-1][t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
			}
			l[t-1] = model.numVar(0, Double.POSITIVE_INFINITY);
		}
		
		// Creating Objective function
		IloLinearNumExpr obj = model.linearNumExpr();
		//online and startup costs
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				obj.addTerm(1, c[g-1][t-1]);
				obj.addTerm(gcp.getOnCost()[g-1], u[g-1][t-1]);
			}
		}
		//production costs
		for (int t=1; t <= gcp.getT(); t++) {
			obj.addTerm(gcp.getShedCost(), l[t-1]);
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				obj.addTerm(gcp.getProdCost()[g-1], p[g-1][t-1]);
			}
		}
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
		//Constraint 1e
		for (int t = 1; t <= gcp.getT(); t++) {
			IloLinearNumExpr lhs = model.linearNumExpr();
			//sum
			for (int g=1; g<=gcp.getnGenerators(); g++) {
				lhs.addTerm(1, p[g-1][t-1]);}
			lhs.addTerm(1, l[t-1]);
			model.addEq(lhs, gcp.getDemand()[t-1]);
		}
		//Constraint 1f
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				lhs.addTerm(1, p[g-1][t-1]);
				lhs.addTerm(-(gcp.getMinP()[g-1]), u[g-1][t-1]);
				model.addGe(lhs, 0);
			}
		}
		//Constraint 1g
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				lhs.addTerm(1, p[g-1][t-1]);
				lhs.addTerm(-(gcp.getMaxP()[g-1]), u[g-1][t-1]);
				model.addLe(lhs, 0);
			}
		}
		//Constraint 1h Note ramping up and down times are the same
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				if (t <= 1) {
					lhs.addTerm(1, p[g-1][t-1]);
				}else {
					lhs.addTerm(1, p[g-1][t-1]);
					lhs.addTerm(-1, p[g-1][t-2]);
				}
				model.addLe(lhs, gcp.getRamping()[g-1]);
			}
		}
		
		//Constraint 1i Note ramping up and down times are the same
		for (int t=1; t <= gcp.getT(); t++) {
			for (int g = 1; g <= gcp.getnGenerators(); g++) {
				IloLinearNumExpr lhs = model.linearNumExpr();
				if (t <= 1) {
					lhs.addTerm(-1, p[g-1][t-1]);
				}else {
					lhs.addTerm(-1, p[g-1][t-1]);
					lhs.addTerm(1, p[g-1][t-2]);
				}
				model.addLe(lhs, gcp.getRamping()[g-1]);
			}
		}

		
	}
	
	private int TU (int t, int gen) {
		return Math.min(t+gcp.getMinT()[gen-1]-1,gcp.getT());
	}
	private int TD (int t, int gen) {
		return Math.min(t+gcp.getMaxT()[gen-1]-1,gcp.getT());
	}
	
	
    public void solve() throws IloException{
        model.solve();
        
        System.out.println(" \n ===> Optimal objective value "+model.getObjValue()+"\n");
    }
    
    public void print() throws IloException {
    	System.out.println("Optimal solution ");
        
        System.out.println("\n=====Shedding===== ");
        System.out.print("[ ");
        for(int i = 1; i<=gcp.getT() ;i++){ 
        	if(model.getValue(l[i-1]) > 0) {
        		System.out.print("l_"+i+" = "+model.getValue(l[i-1])+", ");
        	}
        } System.out.print("] \n");
        
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
        
        /*
        for(int i = 1; i<= gcp.getnGenerators(); i++){
        	System.out.print("[ ");
            for(int j = 1; j<=gcp.getT() ;j++){
            	if(model.getValue(c[i-1][j-1]) != 0) {
            		System.out.print("c_"+i+","+j+" = "+model.getValue(c[i-1][j-1]));
            	}                
            } 
            
        }
        */
    }
    
    public void end(){
        model.end();
    }
}
