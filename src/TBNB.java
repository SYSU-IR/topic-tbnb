import java.io.IOException;
import java.util.ArrayList;


public class TBNB {
	private ArrayList<String> userNameSet;

	public void dataFetching() {
		Collection c = new Collection();
        ArrayList<String> trump = c.read("src\\querys\\queryTrump.txt");
        ArrayList<String> hillary = c.read("src\\querys\\queryHillary.txt");

        if (trump != null) {
            for (String s : trump) {
                //System.out.println(s);
                c.getUser(s, true);
            }
        }

        if (hillary != null) {
            for (String s : hillary) {
                //System.out.println(s);
                c.getUser(s, false);
            }
        }
//        for (String s : userNameSet) {
//            System.out.println(s);
//        }
        System.out.println("Data fetching --- done!");
	}
	
	public void overall() throws IOException {
//		dataFetching();
		TrainingProcess trainingProcess = new TrainingProcess();
		trainingProcess.dataPocessing();
		
		Collection coll = new Collection();
//        userNameSet = coll.getUserForTest();
		coll.getUserForTest();
		userNameSet = coll.readTestUser();
		
		System.out.println("\nExperiment result: ");
        for (int i = 0; i < 20 && i < userNameSet.size(); i++)
		{
        	System.out.println("User: " + i);
			TestProcess testProcess = new TestProcess(trainingProcess);
			String userName = userNameSet.get(i);
			testProcess.checkUserfunction(userName);
		}
	}
	
	public static void main(String[] args) throws IOException {
		new TBNB().overall();
	}

}