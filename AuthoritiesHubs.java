package edu.asu.irs13;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

//Reference : http://www.ccs.neu.edu/course/cs6200f13/proj1.html for Matrix computations

public class AuthoritiesHubs {
	public static void main(String[] args) throws Exception
	{
		// the IndexReader object is the main handle that will give you 
		// all the documents, terms and inverted index
		IndexReader r = IndexReader.open(FSDirectory.open(new File("index")));
			
		int i = 0;
		
		double stamp1,stamp2;
		stamp1 = System.currentTimeMillis();

		// You can find out all the terms that have been indexed using the terms() function
		//Creating an HashMap of HashMap with document id,term and term freq in the respective document
		HashMap<Integer,HashMap<String,Integer>> hMap = new HashMap<Integer,HashMap<String,Integer>>();
		TermEnum t = r.terms();
		while(t.next())
		{
			Term te = new Term("contents", t.term().text());
			TermDocs td = r.termDocs(te);
			
			while(td.next()){
				//if(td.doc() < 25000){				
				int val = td.freq();
				String term = t.term().text();
				HashMap<String,Integer> tMap = hMap.get(td.doc());
				if(tMap==null){
					tMap = new HashMap<String,Integer>();
					tMap.put(term, val);
					hMap.put(td.doc(),tMap);
				}
				else{
					tMap.put(term, val);
				}
			}
			//}
		}
		
		stamp2 = System.currentTimeMillis();
		
		System.out.println("Time for Indexing : " + (stamp2-stamp1)/1000 + " seconds");
		
		//Calculating the docNorm and storing them in an HashMap		
		HashMap<Integer,Double> docNormMap = new HashMap<Integer,Double>();
		Iterator it = hMap.entrySet().iterator();
		
		double stamp3,stamp4;
		stamp3 = System.currentTimeMillis();
		
		while(it.hasNext())
		{
			HashMap.Entry presentIter = (HashMap.Entry)it.next();
		    Integer presentKey = (Integer) presentIter.getKey();
			HashMap <String,Integer> termDocFreq = hMap.get(presentIter.getKey());
			
			Iterator innerIterator = termDocFreq.entrySet().iterator();
			Double docNorm = 0.0;
			
			while(innerIterator.hasNext()){
				
				HashMap.Entry presentInnerIter = (HashMap.Entry)innerIterator.next();
				
				Integer presentFrequencyValue = (Integer) presentInnerIter.getValue(); 
				docNorm +=  presentFrequencyValue * presentFrequencyValue;
			}
			
			docNorm = Math.sqrt(docNorm);
			
			docNormMap.put(presentKey, docNorm);
		}
		
		stamp4 = System.currentTimeMillis();
		
		System.out.println("Time for calculating docNorm : " + (stamp4-stamp2)/1000 + " seconds");
	
		//Taking the quer from the console
		Scanner sc = new Scanner(System.in);
		String str = "";
		System.out.print("query> ");
		
		
		while(!(str = sc.nextLine()).equals("quit"))
		{
			double stamp5,stamp6;
			stamp5 = System.currentTimeMillis();
			
			//Creating an HashMap with the terms in query and termFreq of each term in query
			HashMap<String,Integer> query = new HashMap<String,Integer>();
			Double queryNorm = 0.0;
			String[] terms = str.split("\\s+");
			for(String word : terms)
			{
				int num = 0;
				if(query.get(word)==null){
					num = 1;
				}
				else{
					num = query.get(word);
					num++;
				}
				query.put(word, num);
			}
			
			//Calculating the queryNorm
			Iterator queryIterator = query.entrySet().iterator();
			while(queryIterator.hasNext()){
				
				HashMap.Entry presentQueryIter = (HashMap.Entry)queryIterator.next();
				
				Integer presentQueryFreqValue = (Integer) presentQueryIter.getValue(); 
				queryNorm +=  presentQueryFreqValue * presentQueryFreqValue;
			}
			
			queryNorm = Math.sqrt(queryNorm);
			
			//System.out.println(queryNorm);
			//Making a hashMap with documentId and similarity based on TFIDF
			HashMap<Integer,Double> TFidf = null;
			for(String word : terms)
			{
				double dotProduct;
				double denominator;
				Term term = new Term("contents", word);
				TermDocs tdocs = r.termDocs(term);
				TFidf = new HashMap<Integer,Double>();
				while(tdocs.next())
				{
					//if(tdocs.doc() < 25000){
					if(TFidf.get(tdocs.doc())==null){
						dotProduct = 0;
					}
					else{
						dotProduct = TFidf.get(tdocs.doc());
					}
					HashMap<String,Integer> dotDoc = hMap.get(tdocs.doc());
					dotProduct += query.get(word)* dotDoc.get(word) * Math.log(r.maxDoc()/r.docFreq(term));
					denominator = queryNorm * docNormMap.get(tdocs.doc());
					
					dotProduct = dotProduct/denominator;
					
					TFidf.put(tdocs.doc(), dotProduct);
					}
				//}
			}
			
			//Printing out the 10 results
			int res = 0;
			
			
			for(Integer k : TFidf.keySet()){
				if(res++ == 10)
					break;
				System.out.println(k + "=" + TFidf.get(k));
				
			}
			stamp6 = System.currentTimeMillis();
			System.out.println("Time for retrieving results : " + (stamp6-stamp5)/1000 + " seconds");
			
			
			System.out.println("------------------------------------");
			System.out.println("Results after sorting");
			System.out.println("------------------------------------");
			
			//SOrting the results in order of the similarities and printing out to the console
			Set<Entry<Integer, Double>> set = TFidf.entrySet();
	        List<Entry<Integer, Double>> list = new ArrayList<Entry<Integer, Double>>(set);
	        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>()
	        {
	            public int compare( Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2 )
	            {
	                return (o2.getValue()).compareTo( o1.getValue() );
	            }
	        } );

	        double stamp7,stamp8;
			stamp7 = System.nanoTime();
			
			//Creating root set on value of k(count here)
	        List<Integer> rootSet = new ArrayList<Integer>();
	        int count = 0;
	        for(Map.Entry<Integer, Double> entry:list){
	        	if(count++ == 10)
	        		break;
	        	rootSet.add(entry.getKey()); //adding the pages to create rootset based on k
	            System.out.println(entry.getKey());
	        }
	        
	        stamp8 = System.nanoTime();
			System.out.println("Time for rootSet calculations : " + (stamp8-stamp7) + " seconds");

	        //System.out.println(rootSet);
			
	        
	        LinkAnalysis.numDocs = 25054;
	        LinkAnalysis l = new LinkAnalysis();
	        
	        double stamp9,stamp10;
			stamp9 = System.nanoTime();
			
			//Creating baseset with citations and links in baseset
	        List<Integer> baseSet = new ArrayList<Integer>();
	        
	        for(Integer page : rootSet){
	        	baseSet.add(page);
	        	int[] citations = l.getCitations(page);
	        	for(int x : citations){
	        		if(!baseSet.contains(x)){
	        			baseSet.add(x);
	        		}
	        	}
	        	int[]  links = l.getLinks(page);
	        	for(int y : links){
	        		if(!baseSet.contains(y)){
	        			baseSet.add(y);
	        		}
	        	}
	        	
	        }
	        
	        baseSet = new ArrayList<>(new HashSet<Integer>(baseSet));
	        
	        Collections.sort(baseSet);
	        
	        stamp10 = System.nanoTime();
			System.out.println("Time for baseSet calculations : " + (stamp10-stamp9) + " seconds");
			
			stamp9 = System.nanoTime();
			
			//Creating adjecent matrix
	        double[][] adjMatrix = getadjMatrix(baseSet);
	        
	        stamp10 = System.nanoTime();
			System.out.println("Time for adjMatrix calculations : " + (stamp10-stamp9) + " seconds");

	        double[][] transposeAdjMatrix = new double[baseSet.size()][baseSet.size()];
	        for(int w = 0; w < baseSet.size(); w++){
	        	for(int v = 0; v < baseSet.size(); v++){
	        		transposeAdjMatrix[w][v] = adjMatrix[v][w];
	        	}
	        }
	        
	        
	        //Matrix for hubScore and authScore
	        double[][] hubScore = new double[baseSet.size()][1];
	        double[][] authScore = new double[baseSet.size()][1];
	        
	        for(int t1 = 0;t1<baseSet.size();t1++){
	        	hubScore[t1][0] = 1;
	        	hubScore[t1][0] = 1;
	        }
	        
	        //Treshold for the comparision
	        double treshold = Math.pow(10, -9);
	        int check = 0;
	        int max = 0;
	        double stamp12 = 0,stamp13 = 0,stamp14 = 0;
	        while(true){
	        	
	        	if(max > 1000)break;
	        	
	        	double[][] tempauthScore = new double[baseSet.size()][1];
	        	double[][] temphubScore = new double[baseSet.size()][1];
	        	double[][] subauthScore = new double[baseSet.size()][1];
	        	double[][] subhubScore = new double[baseSet.size()][1];
	        	
	        	//Temporary variables
	        	tempauthScore = authScore;
	        	temphubScore = hubScore;
	        	
	        	
	        	stamp12 = System.nanoTime();
				
	        	authScore = normalize(multiply(transposeAdjMatrix, hubScore));
	        	subauthScore = subtract(authScore, tempauthScore);
	        	
	        	hubScore = normalize(multiply(transposeAdjMatrix, authScore));
	        	subhubScore = subtract(hubScore, temphubScore);
	        	
	        	stamp13 = System.nanoTime();
	        	stamp14 += (stamp13 - stamp12);
				
	        	for(int i1 = 0; i1 < baseSet.size(); i1++){
	        		if(subauthScore[i1][0] > treshold && subhubScore[i1][0] > treshold) {
	        			check = 0;
	        			break;
	        		}
	        		else
	        			check++;        		
	        	}
	        	if(check == baseSet.size())
	        		break;
	        	max++;
	        }
	        
	        System.out.println("Time for norm,mul calculations : " + stamp14/max + " seconds");

	        System.out.println("------------------------------------");
	        System.out.println("------- HUB SCORES -------");
	        System.out.println("------------------------------------");
	        
	        
	        Map<Integer, Double> hubMap = new HashMap<Integer, Double>();
	        for(int i1 = 0; i1 < baseSet.size(); i1++) {
	        	hubMap.put(baseSet.get(i1), hubScore[i1][0]);
	        }
	        
	        int res1 = 0;
			for(Integer k : hubMap.keySet()){
				if(res1++ == 10)
					break;
				System.out.println(k);
			}

			System.out.println("------------------------------------");
			System.out.println("Results after sorting");
			System.out.println("------------------------------------");
			
			
			stamp12 = System.nanoTime();
			//SOrting the results in order of the similarities and printing out to the console
			Set<Entry<Integer, Double>> set1 = hubMap.entrySet();
	        List<Entry<Integer, Double>> list1 = new ArrayList<Entry<Integer, Double>>(set1);
	        Collections.sort(list1, new Comparator<Map.Entry<Integer, Double>>()
	        {
	            public int compare( Map.Entry<Integer, Double> o3, Map.Entry<Integer, Double> o4 )
	            {
	                return (o4.getValue()).compareTo( o3.getValue() );
	            }
	        } );
	        
	        int count1 = 0;
	        for(Map.Entry<Integer, Double> entry:list1){
	        	if(count1++ == 10)
	        		break;
	        	Document doc = r.document(entry.getKey());
	        	String url = doc.getFieldable("path").stringValue();
	            System.out.println(entry.getKey() + "-" + url + "-" + entry.getValue());
	        }
	        
	        stamp13 = System.nanoTime();
	        System.out.println("Time for HUB Sorting calculations : " + (stamp13-stamp12) + " seconds");

			System.out.println("------------------------------------");
	        System.out.println("------- Auth values -------");
	        System.out.println("------------------------------------");
	        
	        Map<Integer, Double> authMap = new HashMap<Integer, Double>();
	        for(int i1 = 0; i1 < baseSet.size(); i1++) {
	        	authMap.put(baseSet.get(i1), authScore[i1][0]);
	        }
	        int res2 = 0;
			for(Integer k : authMap.keySet()){
				if(res2++ == 10)
					break;
				System.out.println(k);
			}
	        
			System.out.println("------------------------------------");
			System.out.println("Results after sorting");
			System.out.println("------------------------------------");
			
			stamp12 = System.nanoTime();
			//SOrting the results in order of the similarities and printing out to the console
			Set<Entry<Integer, Double>> set2 = authMap.entrySet();
	        List<Entry<Integer, Double>> list2 = new ArrayList<Entry<Integer, Double>>(set2);
	        Collections.sort(list2, new Comparator<Map.Entry<Integer, Double>>()
	        {
	            public int compare( Map.Entry<Integer, Double> o5, Map.Entry<Integer, Double> o6 )
	            {
	                return (o6.getValue()).compareTo( o5.getValue() );
	            }
	        } );
	        
	        int count2 = 0;
	        for(Map.Entry<Integer, Double> entry2:list2){
	        	if(count2++ == 10)
	        		break;
	        	Document doc = r.document(entry2.getKey());
	        	String url = doc.getFieldable("path").stringValue();
	            System.out.println(entry2.getKey() + "-" + url + "-" + entry2.getValue());
	        }
	        
	        stamp13 = System.nanoTime();
	        System.out.println("Time for AUTH Sorting calculations : " + (stamp13-stamp12) + " seconds");

	        
			System.out.print("query> ");
			
			TFidf.clear();
			hubMap.clear();
			authMap.clear();
		}		
	}
	
	//Normalizing function
	public static double[][] normalize(double[][] A) {
		int mA = A.length;
        double[][] C = new double[mA][1];
        double sum = 0;
        for(int i = 0;i < mA; i++){
        	sum += A[i][0] * A[i][0];
        }
        for(int i = 0;i < mA; i++){
        	C[i][0] = A[i][0]/Math.sqrt(sum);
        }
        return C;
	}
	
	//Matrix multiplication function
	 public static double[][] multiply(double[][] A, double[][] B) {
	        int mA = A.length;
	        int nA = A[0].length;
	        int mB = B.length;
	        int nB = B[0].length;
	        double[][] C = new double[mA][nB];
	        for (int i = 0; i < mA; i++)
	            for (int j = 0; j < nB; j++)
	                for (int k = 0; k < nA; k++)
	                    C[i][j] += A[i][k] * B[k][j];
	        return C;
	    }
	 	
	 	//Subtraction of matrices function
	    public static double[][] subtract(double[][] A, double[][] B) {
	        int m = A.length;
	        int n = A[0].length;
	        double[][] C = new double[m][n];
	        for (int i = 0; i < m; i++)
	            for (int j = 0; j < n; j++)
	                C[i][j] = Math.abs(A[i][j] - B[i][j]);
	        return C;
	    }
	    
	    //Adjmatrix creating function
	public static double[][] getadjMatrix(List<Integer> baseSet) {
		
		double[][] adjMatrix = new double[baseSet.size()][baseSet.size()];
		LinkAnalysis l = new LinkAnalysis();
		
		for(int i = 0; i < baseSet.size(); i++){			
			int[] l1 = l.getLinks(baseSet.get(i));
			for(int j = 0; j < baseSet.size(); j++) {
				for(int k = 0; k < l1.length; k++) {
					if(l1[k] == baseSet.get(j)){
						adjMatrix[i][j] = 1;
					}
				}
			}
		}
		return adjMatrix;
	}
}
