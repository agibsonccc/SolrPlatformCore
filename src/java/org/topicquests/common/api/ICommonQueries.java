/**
 * 
 */
package org.topicquests.common.api;

import java.util.List;
import java.util.Set;

/**
 * @author park
 *
 */
public interface ICommonQueries {

	/**
	 * Returns <em>all</em> instances of the node identified by <code>locator</code>
	 * @param locator
	 * @param credentials
	 * @return
	 */
	IResult listAllInstances(String locator, Set<String>credentials);
	
	/**
	 * <p>Returns only those nodes which are one of:
	 * <li>not merged</li>
	 * <li>a virtual node for merged nodes</li></p>
	 * @param locator
	 * @param credentials
	 * @return
	 */
	IResult listFilteredInstances(String locator, Set<String>credentials);
	
	IResult listAllSubclasses(String locator, Set<String>credentials);
	
	IResult listFilteredSubclasses(String locator, Set<String>credentials);
}
