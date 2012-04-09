/**
 * 
 */
package com.viewpagerindicator;

import android.view.View;

/**
 * 
 * A ViewProvider provides a complex View to display in the tab title
 * 
 * @author Mehran Maghoumi [aka SirM2X] (maghoumi@gmail.com)
 */
public interface ViewProvider
{
	public View getView(int position);
}
