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

    private static ArrayList<String> getTweet(String name) {
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

    private static Connection getDatabase() {
        Connection db = null;
        try {
            Class.forName("org.postgresql.Driver");
            db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/twitterdb");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver missed!");
            return null;
        } catch (SQLException e) {
            System.out.println("Connection Failed!");
            return null;
        }
        return db;
    }

    public static boolean getUserForTest() {
        Connection db = getDatabase();
        if (db == null)
            return false;

        try {
            PreparedStatement prep = db.prepareStatement("create table testuser (name varchar(100) primary key);");
            prep.executeUpdate();
            prep.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return true;
        }

        try {
            PreparedStatement prep = db.prepareStatement("create table testTwitter (name varchar(100) references testuser(name), tweet varchar(500));");
            prep.executeUpdate();
            prep.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

        ArrayList<String> res = getUserNameByQuery("#Election2016");
        for (String str : res) {
            try {
                PreparedStatement prep = db.prepareStatement("INSERT INTO testuser VALUES (?);");
                prep.setString(1, str);
                prep.executeUpdate();
                prep.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
                continue;
            }

            ArrayList<String> tweets = getTweet(str);
            for (String tweet : tweets) {
                try {
                    PreparedStatement prep = db.prepareStatement("INSERT INTO testTwitter VALUES (?, ?);");
                    prep.setString(1, str);
                    prep.setString(2, tweet);
                    prep.executeUpdate();
                    prep.close();
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
            }
        }

        try {
            db.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }

        return true;
    }

    private static ArrayList<String> readData(String sql, String key) {
        Connection db = getDatabase();
        if (db == null)
            return null;

        ArrayList<String> data = new ArrayList<String>();
        try {
            Statement stmt;
            stmt = db.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String temp = rs.getString(key);
                data.add(temp);
            }
            rs.close();
            stmt.close();
            db.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Operation done successfully");
        return data;
    }

    public static ArrayList<String> readTestUser() {
        String sql = "select * from testuser;";
        return readData(sql, "name");
    }

    public static ArrayList<String> readTestTweet(String name) {
        String sql = "select * from testTwitter where name=\'" + name + "\';";
        return readData(sql, "tweet");
    }

	private static boolean getUser(String keyWord, boolean isTrump) {
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
        Connection db = getDatabase();
        if (db == null)
            return false;

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

    private static ArrayList<String> read(String filePath) {
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
	public static void main(String[] args) {
		/*Collection c = new Collection();
        ArrayList<String> trump = read("queryTrump.txt");
        ArrayList<String> hillary = read("queryHillary.txt");

        if (trump != null) {
            int myCount = 0;
            ArrayList<String> name = new ArrayList<>();
            for (String s : trump) {
                ArrayList<String> temp = c.getUserNameByQuery(s);
                int tempCount = 0;
                for (String str : temp) {
                    if (name.contains(str))
                        continue;
                    name.add(str);
                    tempCount += 20;
                    //ArrayList<String> res = getTweet(str);
                    //tempCount += res.size();
                }
                System.out.println(s + " : " + tempCount);
                myCount += tempCount;
            }
            System.out.println("Trump tot : " + myCount);
        }

        if (hillary != null) {
            int myCount = 0;
            ArrayList<String> name = new ArrayList<>();
            for (String s : hillary) {
                ArrayList<String> temp = c.getUserNameByQuery(s);
                int tempCount = 0;
                for (String str : temp) {
                    if (name.contains(str))
                        continue;
                    name.add(str);
                    tempCount += 20;
                    //ArrayList<String> res = getTweet(str);
                    //tempCount += res.size();
                }
                System.out.println(s + " : " + tempCount);
                myCount += tempCount;
            }
            System.out.println("Hillary tot : " + myCount);
        }*/
        boolean flag = getUserForTest();
        System.out.println(flag);
        ArrayList<String> ts = readTestTweet("kocisue9");
        for (String t : ts) {
            System.out.println(t);
        }
	}
}