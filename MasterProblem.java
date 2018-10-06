import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class MasterProblem {
	
	private IloCplex model;
	private final GeneratorProblem gcp;
	private IloNumVar phi;
	
	public MasterProblem(GeneratorProblem gcp) throws IloException {
		this.model = new IloCplex();
		this.gcp = gcp;
		
		
	}

}
