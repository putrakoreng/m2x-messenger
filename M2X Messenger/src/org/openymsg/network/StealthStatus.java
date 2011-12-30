/**
 * 
 */
package org.openymsg.network;

/**
 * Enumeration of all possible stealth switches 
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 *
 */
public enum StealthStatus
{
	NO_STEALTH (1),
	STEALTH_SESSION(2),
	STEALTH_PERMENANT(3);
	
	private StealthStatus(final int status)
	{
	}

}
