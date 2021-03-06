package de.uni_leipzig.iguana.testcases;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bio_gene.wookie.connection.Connection;
import org.bio_gene.wookie.utils.LogHandler;
import org.jfree.util.Log;

import de.uni_leipzig.iguana.benchmark.Benchmark;
import de.uni_leipzig.iguana.utils.ResultSet;

/**
 * The testcase tests the Querytestcase in a given no of threads
 * it should provide a real case situation
 * 
 * @author Felix Conrads
 */
public class StressTestcase implements Testcase {
	
	public static void main(String[] argc) throws IOException{
		ResultSet res1 = new ResultSet();
		ResultSet res2 = new ResultSet();
		ResultSet res3 = new ResultSet();
		ResultSet res4 = new ResultSet();
		
		List<Object> list = new LinkedList<Object>();
		List<String> head = new LinkedList<String>();
		for(int i=0; i <10;i++){
			list.add(i);
			head.add(String.valueOf(i));
		}
		LinkedList<Object> list2 = new LinkedList<Object>();
		for(int i=10; i >0;i--){
			list2.add(i);
		}
		
		res1.setHeader(head);
		res2.setHeader(head);
		res1.addRow(list);
		res2.addRow(list2);
		res1.addRow(list2);
		res2.addRow(list);
		res1.setFileName("asd_user0");
		res2.setFileName("asd_user1");
		
		
		list = new LinkedList<Object>();
		head = new LinkedList<String>();
		for(int i=0; i <4;i++){
			list.add(i);
			head.add(String.valueOf(i));
		}
		list2 = new LinkedList<Object>();
		for(int i=4; i >0;i--){
			list2.add(i);
		}
		
		res3.setHeader(head);
		res4.setHeader(head);
		res3.addRow(list);
		res4.addRow(list2);
		res3.addRow(list2);
		res4.addRow(list);
		res3.setFileName("asdb_user0");
		res4.setFileName("asdb_user1");
		Collection<ResultSet> res = new LinkedList<ResultSet>();
		res.add(res1);
		res.add(res2);
		res.add(res3);
		res.add(res4);
//		Collection<ResultSet> resM = merge(res);
		for(ResultSet r : res){
			r.save();
		}
//		for(ResultSet r : resM){
//			r.save();
//			r.saveAsPNG();
//		}
	}
	
	
	/** The log. */
	private Logger log;
	
	/** The users. */
	private int users;
	
	/** The props. */
	private Properties props;
	private Properties propsUpdate = new Properties();
	
	/** The res. */
	private Collection<ResultSet> res = new LinkedList<ResultSet>();
	private Collection<ResultSet> resU = new LinkedList<ResultSet>();
	private Collection<ResultSet> resUU = new LinkedList<ResultSet>();

	
	/** The con. */
	private Connection con;
	
	/** The current db name. */
	private String currentDBName;
	
	/** The percent. */
	private String percent;

	private int updateUsers;

	
	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#start()
	 */
	@Override
	public void start() throws IOException {
		log = Logger.getLogger(StressTestcase.class.getName());
		LogHandler.initLogFileHandler(log, StressTestcase.class.getSimpleName());
		Map<String, QueryTestcase> threadPool = new HashMap<String, QueryTestcase>();
		log.info("Initialize users as threads");
		long timeLimit = Long.parseLong(String.valueOf(props.get("time-limit")));
		String queryPatterns = String.valueOf(props.get("queryPatternFile"));
		for(Object key : props.keySet()){
			if(key==null||props.get(key)==null)
				continue;
			propsUpdate.put(key, props.get(key));
		}
		String updateStrategy = String.valueOf(props.get("updateStrategy"));
		int limit =0;
		try{
			limit = Integer.parseInt(String.valueOf(props.get("limit")));
		}
		catch(Exception e){
			limit=5000;
		}
		String ldpath = null;
		try{

			ldpath = String.valueOf(props.get("ldPath"));
			propsUpdate.put("ldPath", props.getProperty("ldPath"));
//			props.remove("ldPath");
		}
		catch(Exception e){
			//NO Live Data
		}
		QueryTestcase.initQH(queryPatterns, updateStrategy, ldpath, limit, log);
		ExecutorService executor = Executors.newFixedThreadPool(users+updateUsers);
		
		
		for(Integer i=0; i<users; i++){
			SPARQLQueryTestcase qt = new SPARQLQueryTestcase();
			qt.setRandomNumber(i+1);
			qt.setConnection(con);
			qt.setCurrentDBName(currentDBName);
			qt.setCurrentPercent(percent);
			qt.setProperties(props);
			
//			Thread t = new Thread(qt);
			Log.info("User "+i+" starting");
			executor.execute(qt);
//			t.start();
			threadPool.put("u"+i, qt);
		}
		for(int i=users; i<updateUsers+users;i++){
//			QueryTestcase qt = new QueryTestcase();
			LiveDataQueryTestcase qt = new LiveDataQueryTestcase();
//			qt.setRandomNumber(i+1);
			qt.setConnection(con);
			qt.setCurrentDBName(currentDBName);
			qt.setCurrentPercent(percent);
			qt.setProperties(propsUpdate);
			if(propsUpdate.containsKey("x")){
				qt.setAmount(Integer.parseInt(String.valueOf(propsUpdate.get("x"))));
			}
			qt.setStrategyRandom(i);
			qt.setStrategy(String.valueOf(propsUpdate.get("updateStrategy")));
//			Thread t = new Thread(qt);
			log.info("Update User "+(i-users)+" starting");
			executor.execute(qt);
			threadPool.put("uu"+i, qt);

		}
		executor.shutdown();
		long time = new Date().getTime();
		while(new Date().getTime()-time<timeLimit){
		}
		for(String t : threadPool.keySet()){
			threadPool.get(t).sendEndSignal();
		}
		while(!executor.isTerminated()){}
//		Boolean alive = true;
//		while(alive){
//			for(Thread t : threadPool.keySet()){
//				if(t.isAlive()){
//					alive= true;
//					break;
//				}
//				else{
//					alive = false;
//				}
//				
//			}
//		}
		log.info("finished stresstest");
		Collection<Collection<ResultSet>> results = new LinkedList<Collection<ResultSet>>();
		Collection<Collection<ResultSet>> results2 = new LinkedList<Collection<ResultSet>>();

		for(String t : threadPool.keySet()){
			if(t.startsWith("uu"))
				results2.add(threadPool.get(t).getResults());
			else
				results.add(threadPool.get(t).getResults());
		}
//		log.info("Merging results");
//		mergeResults(results);
		
		log.info("Saving Results...");
		int user =0;
		for(Collection<ResultSet> resultsUser : results){
			
			for(ResultSet result : resultsUser){
				String file = new File(result.getFileName()).getName();
				file=file.replace("_stresstest", "")+"_stresstest";
				if(user<users+updateUsers)
					file=file.replaceAll("_user[0-9]+", "")+"_user"+user;
				else
					file = file.replaceAll("_userUpdate[0-9]+", "")+"_userUpdate"+(user);
				result.setFileName(File.separator+file);
				try {
					result.save();
				} catch (IOException e) {
					log.severe("Can't save Results due to: ");
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			resU.addAll(resultsUser);
			user++;
		}
		user =0;
		for(Collection<ResultSet> resultsUser : results2){
			
			for(ResultSet result : resultsUser){
				String file = new File(result.getFileName()).getName();
				file=file.replace("_stresstest", "")+"_stresstest";
				if(user<users)
					file=file.replaceAll("_user[0-9]+", "")+"_user"+user;
				else
					file = file.replaceAll("_userUpdate[0-9]+", "")+"_userUpdate"+(user);
				result.setFileName(File.separator+file);
				try {
					result.save();
				} catch (IOException e) {
					log.severe("Can't save Results due to: ");
					LogHandler.writeStackTrace(log, e, Level.SEVERE);
				}
			}
			resUU.addAll(resultsUser);
			user++;
		}
		String dir = Benchmark.TEMP_RESULT_FILE_NAME+File.separator+StressTestcase.class.getName();
		new File(dir).mkdirs();
		for(ResultSet result : res){
			result.setFileName(dir+File.separator+result.getFileName().replace("_stresstest", "")+"_stresstest");
			try {
				result.save();
			} catch (IOException e) {
				log.severe("Can't save Results due to: ");
				LogHandler.writeStackTrace(log, e, Level.SEVERE);
			}
		}
		log.info("...Done saving results");
	}
	
	/**
	 * Merge all the results by mean.
	 *
	 * @param results the results which should be merged together
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private void mergeResults(Collection<Collection<ResultSet>> results){
		List<String>[] headers = new List[results.iterator().next().size()];
		String[] fileNames = new String[results.iterator().next().size()];
		List<List<Object>>[] mergeLists = new List[results.iterator().next().size()];
		Iterator<ResultSet> ir = results.iterator().next().iterator();
		for(int i=0; i<mergeLists.length; i++){
			//init
			ResultSet res = ir.next();
			headers[i] = res.getHeader();
			fileNames[i] = res.getFileName();
			mergeLists[i] = new LinkedList<List<Object>>(res.getTable());
		}
		Iterator<Collection<ResultSet>> icrs = results.iterator();
		icrs.next();
		for(int t=1; t<results.size();t++){
			Collection<ResultSet> resultSets = icrs.next();
			Iterator<ResultSet> irs = resultSets.iterator(); 
			for(int k=0; k< resultSets.size();k++){
				ResultSet res = irs.next();
				int r=0;
				while(res.hasNext()){
					List<Object>current = res.next();
					for(int i=1; i< current.size();i++){
						Integer oldInt = Integer.parseInt(String.valueOf(mergeLists[k].get(r).get(i)));
						Integer newInt = Integer.parseInt(String.valueOf(current.get(i)));
						mergeLists[k].get(r).set(i, oldInt+newInt);
					}
					r++;
				}
			}
		}
		
		//Mean
		for(int k=0; k<mergeLists.length;k++){
			//ResultSet
			for(int t=0;t<mergeLists[k].size();t++){
				//Row
				for(int i=1;i<mergeLists[k].get(0).size();i++){
					//Cell
					Integer mean = Integer.parseInt(String.valueOf(mergeLists[k].get(t).get(i)))/mergeLists.length;
					mergeLists[k].get(t).set(i, mean);
				}
			}
		}
//		res.clear();
		//Set results
		Collection<ResultSet> resNew = new LinkedList<ResultSet>();
		for(int k=0; k<mergeLists.length;k++){
			ResultSet r = new ResultSet();
			for(int t=0;t<mergeLists[k].size();t++){
				r.addRow(mergeLists[k].get(t));
			}
			r.setHeader(headers[k]);
			r.setFileName(fileNames[k]);
			resNew.add(r);
		}
		addCurrentResults(resNew);
	}
	
	public static Collection<ResultSet> merge(Collection<ResultSet> results, String regex, String suffix){
		HashMap<String, Collection<ResultSet>> map = new HashMap<String, Collection<ResultSet>>();
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
		for(ResultSet res : results){
			String parentFile = res.getFileName().replaceAll(regex, "");
			if(!map.containsKey(parentFile)){
				map.put(parentFile, new LinkedList<ResultSet>());
			}
			map.get(parentFile).add(res);
		}
		
		for(String key : map.keySet()){
			ResultSet res = new ResultSet();
			
			res.setFileName(key+suffix);
			Boolean first = true;
			for(ResultSet res2 : map.get(key)){
				ResultSet tmp = new ResultSet();
				
					
				tmp.setTitle(res2.getTitle()+" (Mean of Users)");
				tmp.setxAxis(res2.getxAxis());
				tmp.setxAxis(res2.getyAxis());
				if(first){
					first = false;
					while(res2.hasNext())
						res.addRow(res2.next());
				}else{
					
					while(res2.hasNext()){
						List<Object> rowAdd = new LinkedList<Object>();
						List<Object> row = res2.next();
						List<Object> row2 = res.next();
						//add the rows together
						rowAdd.add(row.get(0));
						for(int i=1;i<row.size();i++){
							double old = Double.valueOf(String.valueOf(row.get(i)));
							double newVal = Double.valueOf(String.valueOf(row2.get(i)));
							rowAdd.add(old+newVal);
									
						}
						tmp.addRow(rowAdd);
					}
					res = tmp;
					res.reset();
				}
				res.setHeader(res2.getHeader());
				
			}
			res.setFileName(key+suffix);
			int divide = Math.max(1, map.size());
			while(res.hasNext()){
				List<Object> row = res.next();
				for(int i=1;i<row.size();i++){
					row.set(i, Double.valueOf(String.valueOf(row.get(i)))/divide);
				}
			}
			res.setFileName(key+suffix);
			ret.add(res);
		}
		
		return ret;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#getResults()
	 */
	@Override
	public Collection<ResultSet> getResults() {
		Collection<ResultSet> ret = new LinkedList<ResultSet>();
//		ret.addAll(res);
		ret.addAll(merge(resU, "_user[0-9]+", "sparql"));
		ret.addAll(resU);
		ret.addAll(merge(resUU, "_userUpdate[0-9]+", "update"));
		ret.addAll(resUU);
		return ret;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		users = Integer.parseInt(String.valueOf(p.get("users")));
		try{
			updateUsers = Integer.parseInt(String.valueOf(p.get("update-users")));
		}catch(Exception e){
			updateUsers = 0;
		}
	
		props = p;
//		props.remove("users");
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#addCurrentResults(java.util.Collection)
	 */
	@Override
	public void addCurrentResults(Collection<ResultSet> currentResults) {
		Iterator<ResultSet> it = currentResults.iterator();
		Iterator<ResultSet> ir = res.iterator();
		Boolean end = !ir.hasNext();
		while(it.hasNext()){
			if(!end && ir.hasNext()){
				ResultSet r = it.next();
				while(r.hasNext()){
					ir.next().addRow(r.next());
				}
			}
			else{
				res.add(it.next());
				end = true;
			}
		}
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setConnection(org.bio_gene.wookie.connection.Connection)
	 */
	@Override
	public void setConnection(Connection con) {
		this.con = con;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentDBName(java.lang.String)
	 */
	@Override
	public void setCurrentDBName(String name) {
		this.currentDBName = name;
	}

	/* (non-Javadoc)
	 * @see de.uni_leipzig.mosquito.testcases.Testcase#setCurrentPercent(java.lang.String)
	 */
	@Override
	public void setCurrentPercent(String percent) {
		this.percent = percent;
	}

}
