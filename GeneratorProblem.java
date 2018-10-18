import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class GeneratorProblem {
	/**
	 * A data-object for containing all the data related to a Generator Commitment Problem with simples getters. 
	 * Values are final and defined at construction. 
	 * The Constructor takes the input files and read them, setting up the problem object
	 */
	
	
	private final int T;
	private final int nGenerators;
	private final double[] startGenCost; // in G
	private final double[] OnCost;    // in G
	private final double[] prodCost;  // in G
	private final int[] minT;	// in G
	private final int[] maxT;	// in G
	private final double[] minP; // in G
	private final double[] maxP; // in G
	private final double[] Demand; // in T
	private final double shedCost;  // in T
	private final double[] ramping;   // in G
	private final String[] name; 	  // in G
	
	public GeneratorProblem(int nGenerator, String loadFile, String generatorFile, int TimeRange, double shedCost) throws FileNotFoundException {
		
		// Initialising
		this.nGenerators = nGenerator;
		this.shedCost = shedCost;
		this.T= TimeRange;
		this.name = new String[this.nGenerators];
		this.minP = new double[this.nGenerators];
		this.maxP = new double[this.nGenerators];
		this.startGenCost = new double[this.nGenerators];
		this.OnCost = new double[this.nGenerators];
		this.ramping = new double[this.nGenerators];
		this.minT = new int[this.nGenerators];
		this.maxT = new int[this.nGenerators];
		this.prodCost = new double[this.nGenerators];
		
		//Read Demand file
		this.Demand = readDemand(loadFile, this.T);
		
		//Reading datatable and assigning values
		File dataFile = new File(generatorFile);
		Scanner dataScanner = new Scanner(dataFile);
		
		System.out.println("Reading Data table:");
		System.out.println(dataScanner.nextLine()); // printing title header
		System.out.println(dataScanner.nextLine()); //printing header line
		
		//assign the value of each column to the data model for each generator and print the row
		for(int g=1; g <=this.nGenerators; g++) {
			System.out.print("Generator "+g+" = ");
			this.name[g-1] = dataScanner.next();
			System.out.print("["+this.name[g-1]);
			this.minP[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.minP[g-1]);
			this.maxP[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.maxP[g-1]);
			this.startGenCost[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.startGenCost[g-1]);
			this.OnCost[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.OnCost[g-1]);
			this.ramping[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.ramping[g-1]);
			this.minT[g-1] = dataScanner.nextInt();
			System.out.print(", "+this.minT[g-1]);
			this.maxT[g-1] = dataScanner.nextInt();
			System.out.print(", "+this.maxT[g-1]);
			this.prodCost[g-1] = Double.parseDouble(dataScanner.next());
			System.out.print(", "+this.prodCost[g-1]+"] \n");
			// The reason for the parseDouble is because the nextDouble() method had issues reading some inputs as doubles.
		}
		dataScanner.close(); // Closing Data table reader
		System.out.println("=====Finished loading data===== \n");
		

	}
	
	private static double[] readDemand(String fileName, int T) throws FileNotFoundException {
    	double[] Demand = new double[T];
    	File file = new File(fileName);
    	Scanner scanner = new Scanner(file);
    	
    	System.out.println("Reading Demand File:");
    	System.out.println(scanner.nextLine());
    	
    	for (int i=1; i<=T; i++) {
    		Demand[i-1] = scanner.nextDouble();
    		System.out.println(Demand[i-1]);
    	}
    	scanner.close();
    	return Demand;
    	
    }
	
	
	

	/**
	 * @return the Time frame, e.g. 24 hours
	 */
	public int getT() {
		return T;
	}

	/**
	 * @return the Number of Generators
	 */
	public int getnGenerators() {
		return nGenerators;
	}

	/**
	 * @return the startGenCost
	 */
	public double[] getStartGenCost() {
		return startGenCost;
	}

	/**
	 * @return the online Cost
	 */
	public double[] getOnCost() {
		return OnCost;
	}

	/**
	 * @return the production Costs of generator i
	 */
	public double[] getProdCost() {
		return prodCost;
	}

	/**
	 * @return the min time a generator i can operate
	 */
	public int[] getMinT() {
		return minT;
	}

	/**
	 * @return the max time a generator i can operate
	 */
	public int[] getMaxT() {
		return maxT;
	}

	/**
	 * @return the min production a generator i can produce
	 */
	public double[] getMinP() {
		return minP;
	}

	/**
	 * @return the max production a generator i can produce
	 */
	public double[] getMaxP() {
		return maxP;
	}

	/**
	 * @return the demand at time t
	 */
	public double[] getDemand() {
		return Demand;
	}

	/**
	 * @return the shedCost
	 */
	public double getShedCost() {
		return shedCost;
	}

	/**
	 * @return the ramping
	 */
	public double[] getRamping() {
		return ramping;
	}

	/**
	 * @return the name
	 */
	public String[] getName() {
		return name;
	}
	
	
	
	
	
}
