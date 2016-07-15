import java.io.IOException;
import java.util.ArrayList;


public class TBNB {
	
	public void dataFetching() {
		Collection c = new Collection();
        //ArrayList<String> trump = read("queryTrump.txt");
        //ArrayList<String> hillary = read("queryHillary.txt");

        /*if (trump != null) {
            for (String s : trump) {
                //System.out.println(s);
                c.getUser(s, true);
            }
        }*/

        /*if (hillary != null) {
            for (String s : hillary) {
                //System.out.println(s);
                c.getUser(s, false);
            }
        }*/
        ArrayList<String> t = c.getUserForTest();
        for (String s : t) {
            System.out.println(s);
        }
	}
	
	public void overall() throws IOException {
		//dataFetching();
		TrainingProcess trainingProcess = new TrainingProcess();
		trainingProcess.dataPocessing();
		
		Collection coll = new Collection();
        ArrayList<String> userNameSet = coll.getUserForTest();
        for (int i =0; i <10; i++)
		{
			TestProcess testProcess = new TestProcess(trainingProcess);
			String userName =userNameSet.get(i);
			testProcess.checkUserfunction(userName);
		}
		/*if (testProcess.probForHillary > testProcess.probForTrump) 
			System.out.println("this user votes for Hillary" );
		else 
			System.out.println("this user votes for Trump");*/
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		new TBNB().overall();
	}

}