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
//        AFRIKAANS("af", "Afrikaans", R.drawable.af),
//        ALBANIAN("sq", "Albanian")
//        AMHARIC("am", "Amharic", R.drawable.am),
//        ARABIC("ar", "Arabic", R.drawable.ar),
//        ARMENIAN("hy", "Armenian"),
//        AZERBAIJANI("az", "Azerbaijani", R.drawable.az),
//        BASQUE("eu", "Basque"),
//        BELARUSIAN("be", "Belarusian", R.drawable.be),
//        BENGALI("bn", "Bengali", R.drawable.bn),
//        BIHARI("bh", "Bihari", R.drawable.bh),
        BULGARIAN("bg", "Bulgarian", R.drawable.bg),
//        BURMESE("my", "Burmese", R.drawable.my),
//        CATALAN("ca", "Catalan", R.drawable.ca),
        CHINESE("zh", "Chinese", R.drawable.cn),
        CHINESE_SIMPLIFIED("zh-CN", "Chinese simplified", R.drawable.cn),
        CHINESE_TRADITIONAL("zh-TW", "Chinese traditional", R.drawable.tw),
        CROATIAN("hr", "Croatian", R.drawable.hr),
        CZECH("cs", "Czech", R.drawable.cs),
//        DANISH("da", "Danish"),
//        DHIVEHI("dv", "Dhivehi"),
        DUTCH("nl", "Dutch", R.drawable.nl),
        ENGLISH("en", "English", R.drawable.us),
//        ESPERANTO("eo", "Esperanto", R.drawable.eo),
//        ESTONIAN("et", "Estonian", R.drawable.et),
//        FILIPINO("tl", "Filipino", R.drawable.tl),
        FINNISH("fi", "Finnish", R.drawable.fi),
        FRENCH("fr", "French", R.drawable.fr),
//        GALICIAN("gl", "Galician", R.drawable.gl),
//        GEORGIAN("ka", "Georgian", R.drawable.ka),
        GERMAN("de", "German", R.drawable.de),
//        GREEK("el", "Greek", R.drawable.el),
//        GUARANI("gn", "Guarani", R.drawable.gn),
//        GUJARATI("gu", "Gujarati", R.drawable.gu),
//        HEBREW("iw", "Hebrew", R.drawable.iw),
//        HINDI("hi", "Hindi", R.drawable.hi),
//        HUNGARIAN("hu", "Hungarian", R.drawable.hu),
//        ICELANDIC("is", "Icelandic", R.drawable.is),
//        INDONESIAN("id", "Indonesian", R.drawable.id),
//        INUKTITUT("iu", "Inuktitut", R.drawable.iu),
        ITALIAN("it", "Italian", R.drawable.it),
//        JAPANESE("ja", "Japanese", R.drawable.ja),
//        KANNADA("kn", "Kannada", R.drawable.kn),
//        KAZAKH("kk", "Kazakh", R.drawable.kk),
//        KHMER("km", "Khmer", R.drawable.km),
//        KOREAN("ko", "Korean", R.drawable.ko),
//        KURDISH("ky", "Kurdish", R.drawable.ky),
//        LAOTHIAN("Laothian", "lo", R.drawable.lo),
//        LATVIAN("Latvian", "la", R.drawable.la),
//        LITHUANIAN("lt", "Lithuanian", R.drawable.lt),
//        MACEDONIAN("mk", "Macedonian", R.drawable.mk),
//        MALAY("ms", "Malay", R.drawable.ms),
//        MALAYALAM("ml", "Malayalam", R.drawable.ml),
//        MALTESE("mt", "Maltese", R.drawable.mt),
//        MARATHI("mr", "Marathi", R.drawable.mr),
//        MONGOLIAN("mn", "Mongolian", R.drawable.mn),
//        NEPALI("ne", "Nepali", R.drawable.ne),
        NORWEGIAN("no", "Norwegian", R.drawable.no),
//        ORIYA("or", "Oriya", R.drawable.or),
//        PASHTO("ps", "Pashto", R.drawable.ps),
//        PERSIAN("fa", "Persian", R.drawable.fa),
        POLISH("pl", "Polish", R.drawable.pl),
        PORTUGUESE("pt", "Portuguese", R.drawable.pt),
//        PUNJABI("pa", "Punjabi", R.drawable.pa),
        ROMANIAN("ro", "Romanian", R.drawable.ro),
        RUSSIAN("ru", "Russian", R.drawable.ru),
//        SANSKRIT("sa", "Sanskrit", R.drawable.sa),
//        SERBIAN("sr", "Serbian", R.drawable.sr),
//        SINDHI("sd", "Sindhi", R.drawable.sd),
//        SINHALESE("si", "Sinhalese", R.drawable.si),
//        SLOVAK("sk", "Slovak", R.drawable.sk),
//        SLOVENIAN("sl", "Slovenian", R.drawable.sl),
        SPANISH("es", "Spanish", R.drawable.es),
//        SWAHILI("sw", "Swahili", R.drawable.sw),
        SWEDISH("sv", "Swedish", R.drawable.sv),
//        TAJIK("tg", "Tajik", R.drawable.tg),
//        TAMIL("ta", "Tamil", R.drawable.ta),
//        TAGALOG("tl", "Tagalog", R.drawable.tl),
//        TELUGU("te", "Telugu", R.drawable.te),
//        THAI("th", "Thai", R.drawable.th),
//        TIBETAN("bo", "Tibetan", R.drawable.bo),
//        TURKISH("tr", "Turkish", R.drawable.tr),
//        UKRAINIAN("uk", "Ukrainian", R.drawable.uk),
//        URDU("ur", "Urdu", R.drawable.ur),
//        UZBEK("uz", "Uzbek", R.drawable.uz),
//        UIGHUR("ug", "Uighur", R.drawable.ug)
        ;

////        ARABIC("ar", "Arabic"), 
//        CHINESE("zh", "Chinese", R.drawable.cn), 
//        CHINESE_SIMPLIFIED("zh-CN", "Chinese simplified", R.drawable.cn),
//        CHINESE_TRADITIONAL ("zh-TW", "Chinese traditional", R.drawable.tw),
//        DUTCH("nl", "Dutch", R.drawable.nl),
//        ENGLISH("en", "English", R.drawable.us),
//        FRENCH("fr", "French", R.drawable.fr), 
//        GERMAN("de", "German", R.drawable.de), 
//        GREEK("el", "Greek", R.drawable.gr), 
//        ITALIAN("it", "Italian", R.drawable.it), 
//        JAPANESE("ja", "Japanese", R.drawable.jp), 
//        KOREAN("ko", "Korean", R.drawable.kr), 
//        PORTUGUESE("pt", "Portuguese", R.drawable.pt),
//        ROMANIAN("ro", "Romanian", R.drawable.ro),
//        RUSSIAN("ru", "Russian", R.drawable.ru), 
//        SPANISH("es", "Spanish", R.drawable.es);
        
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

