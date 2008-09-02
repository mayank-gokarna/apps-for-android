/*
 * Copyright (C) 2008 Google Inc.
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

package com.beust.android.translate;

import com.google.android.collect.Maps;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Button;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Language information for the Google Translate API.
 */
public final class Languages {
    
    static enum Language {
//        ARABIC("ar", "Arabic"), 
        CHINESE("zh", "Chinese", R.drawable.cn), 
        CHINESE_SIMPLIFIED("zh-CN", "Chinese simplified", R.drawable.cn),
        CHINESE_TRADITIONAL ("zh-TW", "Chinese traditional", R.drawable.tw),
        DUTCH("nl", "Dutch", R.drawable.nl),
        ENGLISH("en", "English", R.drawable.us),
        FRENCH("fr", "French", R.drawable.fr), 
        GERMAN("de", "German", R.drawable.de), 
        GREEK("el", "Greek", R.drawable.gr), 
        ITALIAN("it", "Italian", R.drawable.it), 
        JAPANESE("ja", "Japanese", R.drawable.jp), 
        KOREAN("ko", "Korean", R.drawable.kr), 
        PORTUGUESE("pt", "Portuguese", R.drawable.pt),
        RUSSIAN("ru", "Russian", R.drawable.ru), 
        SPANISH("es", "Spanish", R.drawable.es);
        
        private String mShortName;
        private String mLongName;
        private int mFlag;
        
        private static Map<String, String> mLongNameToShortName = Maps.newHashMap();
        private static Map<String, Language> mShortNameToLanguage = Maps.newHashMap();
        
        static {
            for (Language language : values()) {
                mLongNameToShortName.put(language.getLongName(), language.getShortName());
                mShortNameToLanguage.put(language.getShortName(), language);
            }
        }
        
        private Language(String shortName, String longName, int flag) {
            init(shortName, longName, flag);
        }
        
        private void init(String shortName, String longName, int flag) {
            mShortName = shortName;
            mLongName = longName;
            mFlag = flag;
            
        }

        public String getShortName() {
            return mShortName;
        }

        public String getLongName() {
            return mLongName;
        }
        
        public int getFlag() {
            return mFlag;
        }

        @Override
        public String toString() {
            return mLongName;
        }
        
        public static Language findLanguageByShortName(String shortName) {
            return mShortNameToLanguage.get(shortName);
        }
        
        public void configureButton(Activity activity, Button button) {
            button.setTag(this);
            button.setText(getLongName());
            Drawable flag = activity.getResources().getDrawable(getFlag());
            button.setCompoundDrawablesWithIntrinsicBounds(flag, null, null, null);
            button.setCompoundDrawablePadding(5);
        }
    }

    public static String getShortName(String longName) {
        return Language.mLongNameToShortName.get(longName);
    }

    private static void log(String s) {
        Log.d(TranslateActivity.TAG, "[Languages] " + s);
    }

}

