import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class Cardiology_diagnoser
{

	private static int nline=0;
	private static int nrow=0;
	
	private static String[][] fileReader(String filename) throws IOException  //reads and populate a string table with file data
	{
		CSVReader reader = new CSVReader(new FileReader(filename));
		String[][] data;
		String[] line;
		int i=0,j=0;

		nline=0;
		nrow=0;
		//get number of lines
		while((line = reader.readNext()) != null)					
		{
			nline++;
			nrow = line.length;
		}

		reader.close();

		reader = new CSVReader(new FileReader(filename)); //resets pointer position on file
		data = new String[nline][nrow];

		//populate data
		i=0;
		while((line = reader.readNext()) != null) 
		{
			for(j=0;j<nrow;j++)
			{

				data[i][j] = line[j];
			}
			i++;
		}
		return data;
	}

	private static final String getDiagnosis(int[][] knearestneighbours, int ksize, int diagnoserow, String[][] data)
	{
		Hashtable<String, Integer> diagnosesnumber = new Hashtable<String, Integer>(); //to get a position number by the diagnose. To try to stay O(n) complexity
		String[] diagnosescomp = new String[ksize];
		float[] weights = new float[ksize];

		int position;

		int line;
		int row = diagnoserow;
		float heavy =0;
		int heavypos=-1;
		//Comp Diagnosis

		for(int i=0; i<ksize;i++) //initialize vector
		{
			weights[i] = 0;
		}

		for(int i=0; i<ksize; i++) //put weights
		{
			line = knearestneighbours[i][0];
			if(!diagnosesnumber.containsKey(data[line][row])) //New diagnose entry
			{
				//System.out.println(data[line][row]);
				diagnosesnumber.put(data[line][row], i); //add on hashtable
				position = i;
			}
			position = diagnosesnumber.get(data[line][row]);		
			weights[position] += (float) 1 / (knearestneighbours[i][1]+1); //add weight
			diagnosescomp[i] = data[line][row];
			//System.out.println("position " + position + " weight " + weights[position]);
		}

		for(int i=0; i<ksize; i++) //Search for the heaviest diagnosis
		{
			if(weights[i] > heavy)
			{
				heavy = weights[i];
				heavypos=i;
			}
		}

		//System.out.println(" FINAL DIAGNOSIS " + diagnosescomp[heavypos] + " " + heavy);

		return diagnosescomp[heavypos];
	}

	private static int[][] kNearest(int kneighbours, String[][] data, String[] host)
	{
		int[][] result = new int[kneighbours][2];
		int i=0,j=0, k=0;
		int distance;
		boolean updated;

		for(i=0;i<kneighbours;i++) //initialize result
		{
			result[i][0] = 0;
			result[i][1] = -1;
		}

		for(i=1;i<nline;i++) //begin comparison
		{
			updated = false;
			distance = 0; //resets distance info
			for(j=1;j<nrow-1;j++) //compare host to a new line (exclude last row of result)
			{
				//System.out.println(data[i][j] + " " + host[j]);
				if(data[i][j].compareTo(host[j]) != 0)
				{
					distance++;
				}
			}
			//System.out.println("distance = " + distance);
			j=0;
			while(!updated && j<kneighbours) //One major drawback. As I use a sorted insertion to a vector I have to move all k postion in every inserction.
			{								// But as examples is bounded by low k numbers it should be fine.
				if(result[j][1] < 0)
				{
					result[j][0] = i; //line position
					result[j][1] = distance; //stores distance
					updated = true;
				}
				else if(result[j][1] > distance) //overwrite a value
				{
					for(k=j;k<kneighbours-1;k++) //move the vector a postion
					{
						result[k+1][0] = result[k][0];
						result[k+1][1] = result[k][1];
					}
					result[j][0] = i; //overwrite
					result[j][1] = distance;
					updated = true;
				}
				j++;
			}
		}
		return result;
	}

	public static void main(String[] args) throws IOException
	{
		CSVWriter writer = new CSVWriter( new FileWriter("diagnoses.csv"), '\t');
		
		String[][] datasource;
		int source_nline;
		String[][] datatest;
		int test_nline;
		
		int[][] nearestneighbour;

		String actual;
		String computed;

		int truepositive=0;
		int falsepositive=0;
		int truenegative=0;
		int falsenegative=0;

		String[] entries = new String[2];
		
		datasource = fileReader("cardiology_train.csv"); //Reads file
		source_nline = nline;
		datatest = fileReader("cardiology_test.csv");
		test_nline = nline;

		for(int i=1; i<test_nline; i++)
		{
			nearestneighbour = kNearest(3, datasource, datatest[i]);

			actual = datatest[i][7];
			computed = getDiagnosis(nearestneighbour, 3, 7, datasource);

			entries[0] = actual;
			entries[1] = computed;

			writer.writeNext(entries); //Write csv file

			//System.out.println(actual +  " " + computed);

			if(actual.compareTo("Sick") == 0 && computed.compareTo("Sick") == 0) //correct sick status
			{
				//System.out.println("TRUEPOSIYIVE");
				truepositive++;
			}
			else if(actual.compareTo("Sick") == 0) //computed healthy when its sick 
			{
				//System.out.println("FALSENEGATIVE");
				falsenegative++;
			}
			else if(computed.compareTo("Sick") == 0) //computed sick when it's healthy
			{
				//System.out.println("FALSEPOSITIVE");
				falsepositive++;
			}
			else // computed healthy when it's sick
			{
				//System.out.println("TRUENEGATIVE");
				truenegative++;
			}
		}
		writer.close();

		System.out.println("\n");
		System.out.println("Table format:");
		System.out.println("true_positive   false_positive");
		System.out.println("false_negative  true_negative");
		System.out.println();
		System.out.println("Regarding codition Sick:");
		System.out.println( truepositive + " " + falsepositive);
		System.out.println( falsenegative + " " + truenegative);	
		System.out.println();
		System.out.println("Accuracy: " + (float) (truepositive + truenegative) / ( truenegative + falsenegative + truepositive + falsepositive) );
		System.out.println("Precision: " + (float) (truepositive)/(truepositive + falsepositive));
		System.out.println("Sensitivity: " + (float) (truepositive)/(falsenegative + truepositive));
		System.out.println("Specificity: " + (float) (truenegative)/(falsenegative + truenegative));
	}
}