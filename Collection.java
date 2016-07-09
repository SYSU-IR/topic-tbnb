import twitter4j.*;

import java.util.List;
import java.io.*;
import java.sql.*; 
import java.text.*;

public class Collection {

	private static boolean getUser(String keyWord, boolean isTrump) {
		Twitter twitter = new TwitterFactory().getInstance();
        try {
            Query query = new Query(keyWord);
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                	if (!insertUser(tweet.getUser().getScreenName(), isTrump))
                		return false;
                }
            } while ((query = result.nextQuery()) != null);
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            return false;
        }
        return true;
	}

	private static boolean insertUser(String name, boolean isTrump) {
        Connection db = null;
        try {
            Class.forName("org.postgresql.Driver");
            db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/twitterdb");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver missed!");
            return false;
        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            return false;
        }

		try {
			PreparedStatement prep = db.prepareStatement("INSERT INTO twiuser VALUES (?, ?);");
        	prep.setString(1, name);
            int flag = isTrump ? 1 : -1;
			prep.setInt(2, flag);
			prep.executeUpdate();
			prep.close();
		} catch (SQLException e) {
        	System.out.println(e.getMessage());
			return false;
        }

        Twitter twitter = new TwitterFactory().getInstance();
        try {
            List<Status> statuses;
            statuses = twitter.getUserTimeline(name);
            for (Status status : statuses) {
                PreparedStatement prep = db.prepareStatement("INSERT INTO twitter VALUES (?, ?);");
        		prep.setString(1, name);
				prep.setString(2, status.getText());
				prep.executeUpdate();
				prep.close();
            }
            db.close();
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            return false;
        } catch (SQLException e) {
        	System.out.println(e.getMessage());
			return false;
        }

        return true;
	}

	public static void main(String[] args) {
		Collection c = new Collection();
        c.getUser("CrookedHillary", true);
        c.getUser("NoTrump", false);
		FileReader file = new FileReader("queryTrump.txt");
		try {
			BufferedReader br = new BufferedReader(file);
        	while(br.readLine() != null){
            	String s = br.readLine();
            	if (getUser(s))
            		System.out.println(s + " succeeds");
            	else
            		System.out.println(s + " fails");
        	}
        	br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}