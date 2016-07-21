import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.lang.*;

import org.netlib.util.doubleW;
import org.netlib.util.intW;

import sun.awt.image.ToolkitImage;

import cc.mallet.examples.TopicModel;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

public class TestProcess {
	public double probForTrump;
	public double probForHillary;
	private InstanceList tempInstances;
	private TrainingProcess trainingProcess;
	
	//
	private int h = 0;
	private int t = 0;
	
	public TestProcess(TrainingProcess t) {
		trainingProcess = t;
		probForHillary = 0;
		probForTrump = 0;
	}
	

	public void checkInstanceFunction(Instance tweet) {
        /*
        In this function, we use a instance to calculate the probabilities that if the user
        is belong to which types.
        1. use Gibbs sampling to analyze the topicDistribution of instance ,decide 
            which topic the instance is belong to, and get the corresponding probability
            table of the topic.
        2. for every word(token) in the instance, if it is not in the vocabulary of this
            topic, we ignore this word. If it is in that topic, calculate the logarithm
            of probabilities that the word is belong to each type, and add them to 
            corresponding accumulator.
        */

        TopicInferencer infer = trainingProcess.getModel().getInferencer();

        FeatureSequence tokens = (FeatureSequence) tweet.getData();

        double[] probabilities = infer.getSampledDistribution(tweet, 10, 1, 5);

   		  double max = 0;
   		  int topicIndex = 0;
       	for (int i = 0; i < probabilities.length; ++i) {
       		if (probabilities[i] > max) {
       			max = probabilities[i];
       			topicIndex = i;
       		}
       	}

//        System.out.println("topicIndex: " + topicIndex + " max: " + max + " probabilities[0]: " + probabilities[0]);
       	
       	//
       	
       	HashMap<String, tokenProbility> coreTopic = trainingProcess.getProbilityTable(topicIndex);
       	String token;
       	Alphabet dataAlphabet = tempInstances.getDataAlphabet();
       	for (int i = 0; i < tokens.getLength(); ++i) {
       		token = (String) dataAlphabet.lookupObject(tokens.getIndexAtPosition(i));
       		if (coreTopic.containsKey(token)) {
       			probForHillary += Math.log(coreTopic.get(token).getForHillary());
       			probForTrump +=Math.log(coreTopic.get(token).getForTrump());
       			
       			if (coreTopic.get(token).getForHillary() > coreTopic.get(token).getForTrump()) {
       				h++;
       			}
       			else {
       				t++;
       			}
       		}
       	}
    }

    public void checkUserfunction(String userName) {
        /*
        In this function, we read a document that containing all tweets of a user,
        and call the method checkInstanceFunction() to check which type this user is.
        1. call the method ??? to get a document of user
        2. call the method checkInstanceFunction
        */

        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
          // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File("src\\stoplists\\en.txt"), "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );
        
        tempInstances = new InstanceList (new SerialPipes(pipeList));
    
        Collection colle = new Collection();
//        String userName =userNameSet.get(i);
        ArrayList<String> testSet = colle.getTweet(userName);

        for (int i = 0; i < testSet.size(); ++i)
          tempInstances.addThruPipe(new Instance(testSet.get(i), 3, 2, 1));
          
        int size = tempInstances.size();
//        System.out.println("size of the InstanceList: " + size);
        for (int j = 0; j < size; ++j) {
          checkInstanceFunction(tempInstances.get(j));
          /*tempInstances.remove(0);*/
        }
        System.out.println(userName);
        System.out.println("Hillary: " + probForHillary + "\t" + "Trump: " + probForTrump);
        
        System.out.println("trump: " + t + " hillary: " + h);
        
        if (probForHillary > probForTrump) 
          System.out.println("this user votes for Hillary\n" );
        else 
          System.out.println("this user votes for Trump\n");
    }

//    public static void main(String[] args) throws IOException {
////		String alldata = "C:\\Users\\Shower\\Desktop\\collection\\data\\alltweets.txt";
//		TrainingProcess trainingProcess = new TrainingProcess();
//		trainingProcess.dataPocessing();
//		
//		TestProcess testProcess = new TestProcess(trainingProcess);
//		testProcess.checkUserfunction();
//		if (testProcess.probForHillary > testProcess.probForTrump) 
//			System.out.println(" this user is vote for Hillary" );
//		else 
//			System.out.println("this user is vote for Trump");
//	}
}