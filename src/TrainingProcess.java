import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.*;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;


public class TrainingProcess {
	private InstanceList instances;
	private ParallelTopicModel model;
	private int numTopics;
	private ArrayList<HashMap<String, tokenProbility> > topicProbilityTable;
	
	public InstanceList getInstanceList() {
		return instances;
	}
	public ParallelTopicModel getModel() {
		return model;
	}
	public HashMap<String, tokenProbility> getProbilityTable(int index) {
		return topicProbilityTable.get(index);
	}
	
	public TrainingProcess() {
 		numTopics = 10;
 		topicProbilityTable = new ArrayList<HashMap<String, tokenProbility>>();
 		for (int i = 0; i < numTopics; ++i) {
			topicProbilityTable.add(new HashMap<String, tokenProbility>());
		}
	}
	
	public InstanceList createInstanceList(String sql) throws UnsupportedEncodingException, FileNotFoundException {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(new File("src\\stoplists\\en.txt"), "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence() );
        
        InstanceList tempInstances = new InstanceList (new SerialPipes(pipeList));
        
        ArrayList<String> trainingSet = readData(sql);
        for (int i = 0; i < trainingSet.size(); ++i) {
        	tempInstances.addThruPipe(new Instance(trainingSet.get(i), 3, 2, 1));
        }
        
        return tempInstances;
	}

	public ArrayList<String> readData(String sql) {
		Connection db = connectDB();
		if (db == null) {
			return null;
		}
		ArrayList<String> data = new ArrayList<String>();
		try {
			Statement stmt;
			stmt = db.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String tweet = rs.getString("tweet");
//				System.out.println(tweet);
//				System.out.println();
				data.add(tweet);
			}
			rs.close();
			stmt.close();
		} catch (SQLException e) {
        	System.out.println(e.getMessage());
        }
        System.out.println("Operation done successfully");
        return data;
	}
	

	public void topicGeneration() throws IOException {
		 // Create a model with 10 topics, alpha_t = 0.01, beta_w = 0.01
 		//  Note that the first parameter is passed as the sum over topics, while
 		//  the second is 
 		model = new ParallelTopicModel(numTopics, 1.0, 0.01);

 		model.addInstances(instances);

 		// Use two parallel samplers, which each look at one half the corpus and combine
 		//  statistics after every iteration.
 		model.setNumThreads(2);

 		// Run the model for 50 iterations and stop (this is for testing only, 
 		//  for real applications, use 1000 to 2000 iterations)
 		//
 		model.setNumIterations(1500);
 		model.estimate();
	}
	
	public void topicInferencing() throws UnsupportedEncodingException, FileNotFoundException {
   		String sql1 = "SELECT t.tweet FROM twitter t, twiuser u WHERE t.user = u.user AND u.flag LIKE " + "\'1%\';";
		String sql2 = "SELECT t.tweet FROM twitter t, twiuser u WHERE t.user = u.user AND u.flag LIKE " + "\'-1%\';";
		
		inferencing(sql1, 1);
		inferencing(sql2, -1);
	}
	
	public void inferencing(String sql, int flag) throws UnsupportedEncodingException, FileNotFoundException {
		TopicInferencer inferencer = model.getInferencer();
		InstanceList instanceList = createInstanceList(sql);
		
   		for (int j = 0; j < instanceList.size(); ++j) {
   			double[] probabilities = inferencer.getSampledDistribution(instanceList.get(j), 10, 1, 5);
   			double max = 0;
   			int topicIndex = 0;
       		for (int i = 0; i < probabilities.length; ++i) {
       			if (probabilities[i] > max) {
       				max = probabilities[i];
       				topicIndex = i;
       			}
       		}
       		createProbilityTable(topicIndex, instanceList.get(j), flag, instanceList);
   		}
	}
	
	public void createProbilityTable(int topicIndex, Instance ins, int flag, InstanceList list) throws UnsupportedEncodingException, FileNotFoundException {
		//  获取主题特征词
		HashMap<String, Double> topicWords = getTopicWords(topicIndex);
		//  记录用过的词
		ArrayList<String> used = new ArrayList<String>();
//		Alphabet dataAlphabet = instances.getDataAlphabet();
		Alphabet dataAlphabet = list.getDataAlphabet();
		//  instance的tokens
		FeatureSequence tokens = (FeatureSequence) ins.getData();
		String token;
		for (int i = 0; i < tokens.getLength(); ++i) {
			token = (String)dataAlphabet.lookupObject(tokens.getIndexAtPosition(i));
			if (topicWords.containsKey(token)) {
				if (used.contains(token)) {
					continue;
				}
				else {
					used.add(token);
				}
				
				double cnt = 0.0;
				for (int j = 0; j < tokens.getLength(); ++j) {
					if (token.equals((String)dataAlphabet.lookupObject(tokens.getIndexAtPosition(j)))) {
						cnt++;
					}
				}
				double pro = cnt;
				double smallest = 0.0;
				
				HashMap<String, tokenProbility> topic = topicProbilityTable.get(topicIndex);
				//trump
				if (flag == 1) {
					if (topic.containsKey(token)) {
						double old = topic.get(token).getForTrump();
						topic.get(token).setForTrump((pro + old));
					}
					else {
						topic.put(token, new tokenProbility(smallest, pro));
					}
				}
				//hillary
				else if(flag == -1) {
					if (topic.containsKey(token)) {
						double old = topic.get(token).getForHillary();
						topic.get(token).setForHillary(pro + old);
					}
					else {
						topic.put(token, new tokenProbility(pro, smallest));
					}
				}
			}
		}
	}
	
	public void computProbability() {
		for (HashMap<String, tokenProbility> m : topicProbilityTable) {
			for (String s : m.keySet()) {
				tokenProbility tem = m.get(s);
				double h = tem.getForHillary();
				double t = tem.getForTrump();
				double total = h + t;
				tem.setForHillary((h + 1) / (total + 1000));
				tem.setForTrump((t + 1) / (total + 1000));
			}
		}
	}
	
	
	public void showTopics() {
		// The data alphabet maps word IDs to strings
     	Alphabet dataAlphabet = instances.getDataAlphabet();
		
		double[] topicDistribution = model.getTopicProbabilities(0);
		
		// Get an array of sorted sets of word ID/count pairs
     	ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		// Show top 5 words in topics with proportions for the first document
     	for (int topic = 0; topic < numTopics; topic++) {
     		Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
     			
     		out = new Formatter(new StringBuilder(), Locale.US);
     		out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
     		int rank = 0;
     		while (iterator.hasNext() && rank < 5) {
     			IDSorter idCountPair = iterator.next();
     			out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
     			rank++;
    		}
     		System.out.println(out);
    	}
	}
	
	public HashMap<String, Double> getTopicWords(int index) {
		// The data alphabet maps word IDs to strings
     	Alphabet dataAlphabet = instances.getDataAlphabet();

     	// Get an array of sorted sets of word ID/count pairs
     	TreeSet<IDSorter> topicSortedWords = model.getSortedWords().get(index);
 		Iterator<IDSorter> iterator = topicSortedWords.iterator();

     	int rank = 0;
     	HashMap<String, Double> topicWords = new HashMap<String, Double>();
     	//
     	while (iterator.hasNext() && rank < 1000) {
     		IDSorter idCountPair = iterator.next();
     		topicWords.put((String)dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
     		rank++;
     	}
     	return topicWords;
	}
	
	public void dataPocessing() throws IOException {
		String sql = "SELECT tweet FROM twitter;";
		instances = createInstanceList(sql);
		topicGeneration();
//		showTopics();
		topicInferencing();
		computProbability();
		debug();
    }
	
	public void debug() {
		int j = 0;
		int hillary = 0;
		int trump = 0;
		
		for (HashMap<String, tokenProbility> t : topicProbilityTable) {
			int tt = 0;
			int h = 0;
			for (String i : t.keySet()) {
//				System.out.println(i + "\tTrump: " + t.get(i).getForTrump() + " Hillary: " +
//						t.get(i).getForHillary());
				if (t.get(i).getForTrump() > t.get(i).getForHillary()) {
//					System.out.println(i + ": trump");
					trump++;
					tt++;
				}
				else if (t.get(i).getForTrump() < t.get(i).getForHillary()) {
//					System.out.println(i + ": hillary");
					hillary++;
					h++;
				}
			}
			System.out.println(j + " trump: " + tt + " hillary: " + h);
			j++;
			
		}
		System.out.println("trump: " + trump + " hillary: " + hillary);
	}

	public Connection connectDB() {
		Connection db = null;
        try {
            Class.forName("org.postgresql.Driver");
            db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/twitterdb", "postgres", "123456");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver missed!");
        } catch (SQLException e) {
            System.out.println("Connection Failed!");
        }
        return db;
	}
//	public static void main(String[] args) throws IOException {		
//		TrainingProcess trainingProcess = new TrainingProcess();
//		trainingProcess.dataPocessing();
//	}
}



















