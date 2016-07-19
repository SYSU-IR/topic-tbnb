import twitter4j.*;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.sql.*; 
import java.text.*;

public class Collection {

    private static ArrayList<String> getUserNameByQuery(String keyWord) {
        Twitter twitter = new TwitterFactory().getInstance();
        ArrayList<String> res = new ArrayList<>();
        try {
            Query query = new Query(keyWord);
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status status : tweets) {
                    res.add(status.getUser().getScreenName());
                }
            } while ((query = result.nextQuery()) != null);
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            return null;
        }
        return res;
    }

    public static ArrayList<String> getTweet(String name) {
        Twitter twitter = new TwitterFactory().getInstance();
        ArrayList<String> res = new ArrayList<>();
        try {
            List<Status> statuses;
            statuses = twitter.getUserTimeline(name);
            for (Status status : statuses) {
                res.add(status.getText());
            }
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
            return null;
        }
        return res;
    }

    public static ArrayList<String> getUserForTest() {
        ArrayList<String> res = getUserNameByQuery("#Election2016");
        return res;
    }

	public static boolean getUser(String keyWord, boolean isTrump) {
        ArrayList<String> res = getUserNameByQuery(keyWord);
        if (res == null)
            return false;

        for (String name : res) {
            if (!insertUser(name, isTrump))
                        return false;
        }

        return true;
	}

	private static boolean insertUser(String name, boolean isTrump) {
        Connection db = null;
        try {
            Class.forName("org.postgresql.Driver");
//            db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/twitterdb");
            db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/twitterdb", "postgres", "123456");
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

        ArrayList<String> tweets = getTweet(name);
        try {
            for (String tweet : tweets) {
                PreparedStatement prep = db.prepareStatement("INSERT INTO twitter VALUES (?, ?);");
                prep.setString(1, name);
                prep.setString(2, tweet);
                prep.executeUpdate();
                prep.close();
            }
            db.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
	}

    public static ArrayList<String> read(String filePath) {
        ArrayList<String> temp = new ArrayList<>();
        try {
            FileReader file = new FileReader(filePath);
            BufferedReader br = new BufferedReader(file);
            String s = br.readLine();
            while(s != null){
                temp.add(s);
                s = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return temp;
    }
//
//	public static void main(String[] args) {
//		Collection c = new Collection();
//        //ArrayList<String> trump = read("queryTrump.txt");
//        //ArrayList<String> hillary = read("queryHillary.txt");
//
//        /*if (trump != null) {
//            for (String s : trump) {
//                //System.out.println(s);
//                c.getUser(s, true);
//            }
//        }*/
//
//        /*if (hillary != null) {
//            for (String s : hillary) {
//                //System.out.println(s);
//                c.getUser(s, false);
//            }
//        }*/
//        ArrayList<String> t = getUserForTest();
//        for (String s : t) {
//            System.out.println(s);
//        }
//	}
}