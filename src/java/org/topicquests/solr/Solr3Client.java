/*
 * Copyright 2012, TopicQuests
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.topicquests.solr;
import java.util.*;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
//import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.schema.DateField;

import org.topicquests.common.api.IResult;
import org.topicquests.common.api.ITopicQuestsOntology;
import org.topicquests.common.ResultPojo;
import org.topicquests.solr.api.ISolrClient;

/**
 * @author park
 * For non-SolrCloud use
 */
public class Solr3Client implements ISolrClient {
	private HttpSolrServer server;
	/**
	 * @param solrURL
	 * @throws Exception
	 */
	public Solr3Client(String solrURL) throws Exception {
		System.out.println("SERVER "+solrURL);
		server = new HttpSolrServer(solrURL);
	//	server.setParser(new XMLResponseParser());
	}

	public SolrServer getSolrServer() {
		return server;
	}
	
	/**
	 * Run a query based on <code>queryString</code>
	 * @param queryString
	 * @param start TODO
	 * @param count TODO
	 * @return  NamedList<Object> in result or error string
	 */
	public IResult runQuery(String queryString, int start, int count) {
		System.out.println("Solr3Client.runQuery- "+queryString+" "+start+" "+count);
		IResult result = new ResultPojo();
		SolrQuery parameters = new SolrQuery();
		parameters.set("q", queryString);
		parameters.setStart(start);
		if (count > -1)
			parameters.setRows(count);
		//force result as JSON
//		parameters.set("wt", "json");
		System.out.println("Solr3Client.runQuery-1 "+parameters.toString());
		try {
			QueryResponse x = server.query(parameters);
			System.out.println("ZZZZ "+x.getResults());
			result.setResultObject(x.getResults());
		} catch (Exception e) {
			//TODO log the error
			e.printStackTrace();
			result.addErrorString(e.getMessage());
		}
		return result;
	}
	
	/**
	 * Update has the effect of removing then replacing a document
	 * @param fields
	 * @return
	 * @deprecated
	 */
	public IResult updateData(Map<String,Object>fields) {
		return addData(fields);
	}
	

	/**
	 * Removes an entire document //TODO needs testing
	 * @param locator
	 * @return
	 */
	public IResult deleteByLocator(String locator) {
		IResult result = new ResultPojo();
		try {
			UpdateResponse ur = server.deleteById(locator);
			int status = ur.getStatus();
			server.commit();
			result.setResultObject(new Integer(status));
		} catch (Exception e) {
			result.addErrorString(e.getMessage());
		}
		return result;
		
	}
	/**
	 * Add data based on fields and values in <code>fields</code>
	 * @param fields
	 * @return <code>null</code> or status code as Integer if error with error string
	 * TODO: SolrInputDocument.putAll(Map<String,Object) might work in some cases
	 */
	public IResult addData(Map<String,Object>fields) {
		IResult result = new ResultPojo();
		if (fields.isEmpty()) {
			//result.addErrorString("SolrClient got an empty document");
			return result;
		}
		System.out.println("Solr3Client.addData-1 "+fields.size());
		int status = 0;
		try {
			SolrInputDocument document = mapToDocument(fields);
			UpdateResponse response = server.add(document);
			status = response.getStatus();
			//TODO full commit or soft commit?
			server.commit();
		} catch (Exception e) {
			result.addErrorString(e.getMessage());
			System.out.println("Solr3Client.addData error "+e.getMessage());
			e.printStackTrace();
		}
		result.setResultObject(new Integer(status));
		return result;
	}
	
	@Override
	public IResult addData(Collection<Map<String, Object>> documents) {
		IResult result = new ResultPojo();
		Iterator<Map<String,Object>>itr = documents.iterator();
		int status = 0;
		try {
			SolrInputDocument document = null;
			Map<String,Object>fields = null;
			List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
			while (itr.hasNext()) {
				fields = itr.next();
				if (!fields.isEmpty()) {
					document = mapToDocument(fields);
					docs.add(document);
				} //else
					// result.addErrorString("SolrClient got an empty document");
			}
			UpdateResponse response = server.add(docs);
			status = response.getStatus();
			//TODO full commit or soft commit?
			server.commit();
		} catch (Exception e) {
			result.addErrorString(e.getMessage());
			System.out.println("Solr3Client.addData error "+e.getMessage());
			e.printStackTrace();
		}
		result.setResultObject(new Integer(status));

		return result;
	}

	/**
	 * <p>Convert a Map of fields to a {@link SolrInputDocument}</p>
	 * <p>Must deal with a variety of field types</p>
	 * @param fields
	 * @return
	 * @throws Exception
	 */
	SolrInputDocument mapToDocument(Map<String,Object> fields) throws Exception {
		SolrInputDocument document = new SolrInputDocument();
		Iterator<String>keys = fields.keySet().iterator();
		String key;
		List<String>lobj;
		Object o;
		while (keys.hasNext()) {
			key = keys.next();
			o = fields.get(key);
			System.out.println("Solr3Client.addData-2 "+key+" | "+o);
			//here we try to catch the obvious ones
			//TODO expand the test for Float
			if (o instanceof String ||
				o instanceof Double ||
				o instanceof Boolean ||
				o instanceof Map || // required for partial updates only
				o instanceof Long ||
				o instanceof Float
				) {
//				System.out.println("Solr3Client.addData-2 "+o); 
					document.addField(key, o);
			} else if (o instanceof List) {
//				System.out.println("Solr3Client.addData-2list "+o); 
				lobj = (List<String>)o;
				Iterator<String>itr = lobj.iterator();
				while (itr.hasNext()) {
					//TODO if the list is just a list of single strings, e.g. mostly locators,
					//then we don't want to escape them
					//document.addField(key,  ClientUtils.escapeQueryChars(vx));
					//document.addField(key, escapeQueryCulprits(vx));
					//unit tests suggest we don't need to do any escapes
					//TODO pay attention to this issue
					document.addField(key, itr.next());
				} 
			} else if (key.equals(ITopicQuestsOntology.CREATED_DATE_PROPERTY) ||
					   key.equals(ITopicQuestsOntology.LAST_EDIT_DATE_PROPERTY)) {
					document.addField(key,  DateField.formatExternal((Date)o));
			} else
				throw new Exception ("Solr3Client.addData fail: "+key+" "+o);
		}		
		return document;
	}
	/**
	 * Fetch by way of the node's <em>locator</em> field
	 * @param locator
	 * @param start
	 * @param count
	 * @return
	 */
	public IResult getByProxyLocator(String locator, int start, int count) {
		String q = ITopicQuestsOntology.LOCATOR_PROPERTY+":"+locator;
		System.out.println("QQQ "+q);
		IResult result = runQuery(q, start, count);
		return result;		
	}

	@Override
	public IResult partialUpdateData(Map<String, Object> fields) {
		return addData(fields);
	}

	@Override
	public void shutDown() {
		server.shutdown();
	}
/*
	 String escapeQueryCulprits(String s)
	 {
	 StringBuilder sb = new StringBuilder();
	 for (int i = 0; i < s.length(); i++)
	 {
	 char c = s.charAt(i);
	 // These characters are part of the query syntax and must be escaped
	 if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
	 || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
	 || c == '*' || c == '?' || c == '|' || c == '&' || c == ';'
	 )
	 {
	 sb.append('\\');
	 }
	 if(Character.isWhitespace(c))
	 {
	 sb.append(" \\ ");
	 }
	 sb.append(c);
	 }
	 return sb.toString();
	 }
*/
}