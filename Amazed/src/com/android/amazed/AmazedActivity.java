/*
 * Copyright (C) 2008 Jason Tomlinson.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.amazed;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/*
 * Activity responsible for controlling the application.
 */
public class AmazedActivity extends Activity {
    
	// application context
	static Context context;

	// custom view
	AmazedView view;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        
        // remove title bar.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // setup our view, give it focus and display.
       	view = new AmazedView(context, this);
       	view.setFocusable(true);
        setContentView(view); 
    }
	
    @Override
    protected void onResume()
    {
        super.onResume();
        view.registerListener();      
    }
    
    @Override
    public void onSaveInstanceState(Bundle icicle) 
    {
    	super.onSaveInstanceState(icicle);
    	view.unregisterListener();
    }
}