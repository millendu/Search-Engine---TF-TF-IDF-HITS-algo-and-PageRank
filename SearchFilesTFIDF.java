package edu.asu.irs13;

import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.QueryParserTokenManager;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;


public class SearchFilesTFIDF {
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
			
			double stamp7,stamp8;
			stamp7 = System.currentTimeMillis();
			
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
	        int count = 0;
	        for(Map.Entry<Integer, Double> entry:list){
	        	if(count++ == 10)
	        		break;
	        
	            System.out.println(entry.getKey());
	        }
	        stamp8 = System.currentTimeMillis();
			System.out.println("Time for sorting the retrieved results : " + (stamp8-stamp7)/1000 + " seconds");
			
			System.out.print("query> ");
			
			TFidf.clear();
			
		}
		
	}
}
