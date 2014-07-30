/*
* $Id: Rexx.java,v 1.15 2011-10-28 08:06:16 pgr Exp $
*
* (C) Copyright 2011 Jaxo Inc.  All rights reserved.
* This work contains confidential trade secrets of Jaxo.
* Use, examination, copying, transfer and disclosure to others
* are prohibited, except with the express written agreement of Jaxo.
*
* Author:  Pierre G. Richard
* Written: 07/25/2011
*/
package com.jaxo.android.rexx;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

/*-- class Rexx --+
*//**
*
* @author  Pierre G. Richard
* @version $Id: Rexx.java,v 1.15 2011-10-28 08:06:16 pgr Exp $
*/
public class Rexx extends Activity
{
   static {
      System.loadLibrary("reslib");
      System.loadLibrary("decnblib");
      System.loadLibrary("toolslib");
      System.loadLibrary("rexxlib");
      System.loadLibrary("androidlib");
   }
   public static String SCRIPT_CONTENT_KEY = "jaxo.rexx.script";   // String
   public static String RESULT_KEY = "jaxo.rexx.result";           // String
   public static String REXX_MESSAGE_KEY = "jaxo.rexx.message";    // String
   public static String REXX_ERRORCODE_KEY = "jaxo.rexx.rexxcode"; // int
   public static int RESULTCODE_OK = RESULT_FIRST_USER;
   public static int RESULTCODE_ERROR = RESULT_FIRST_USER + 1;
   public static int RESULTCODE_EXCEPTION_THROWN = RESULT_FIRST_USER + 2;

   private Speaker m_speaker;

   @Override
   /*----------------------------------------------------------------onCreate-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.console);
      setTitle(R.string.RexxInterpreter);

      final String content;
      Intent intent = getIntent();
      if (intent.getAction() == null) {  // explicit (from ScriptEditor)
         Bundle extras = getIntent().getExtras();
         if ((extras != null) && extras.containsKey(SCRIPT_CONTENT_KEY)) {
            content = extras.getString(SCRIPT_CONTENT_KEY);
         }else {
            content = null;
         }
      }else {                            // implicit
         content = getIntentData(intent);
      }

      m_speaker = new Speaker(Rexx.this);
      if (content != null) {
         new Thread() {
            public void run() {
               try {
                  runInterpretThread(
                     content,
                     new URI("file://" + getFilesDir().getAbsolutePath() + "/")
                  );
               }catch (Exception e) {          // hope not!
                  setResult(RESULT_CANCELED);
                  finish();
               }
            }
         }.start();
      }else {
         setResult(RESULT_CANCELED);
         finish();
      }
   }

   @Override
   /*---------------------------------------------------------------onDestroy-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public void onDestroy() {
      super.onDestroy();
      m_speaker.close();
   }
   /*------------------------------------------------------runInterpretThread-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private void runInterpretThread(String content, URI baseUri) {
      RexxConsole console = new RexxConsole(
         (Activity)Rexx.this,
         baseUri,
         (TextView)findViewById(R.id.say_view)
      );
      int rc = interpret(
         content,
         console,
         baseUri.toString(),
         m_speaker
      );
      console.flush();
      int resultCode;
      Intent intent = new Intent();
      intent.putExtra(RESULT_KEY, console.m_result);
      if (rc == 0) {
         resultCode = RESULTCODE_OK;
      }else {
         intent.putExtra(REXX_ERRORCODE_KEY, rc);
         if (rc == -1) {
            resultCode = RESULTCODE_EXCEPTION_THROWN;
         }else { // rc has the standard Rexx value of the error
            resultCode = RESULTCODE_ERROR;
         }
      }
      setResult(resultCode, intent);
      if (rc != 0) finish();
   }

   /*-----------------------------------------------------------getIntentData-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private String getIntentData(Intent intent) {
      String contents = null;
      Uri uri;
      if ((uri=intent.getData()) != null) {
         try {
            byte[] data = null;
            InputStream in;
            long len;
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
               ContentResolver solver = getContentResolver();
               len = solver.openAssetFileDescriptor(uri, "r").getLength();
               in = solver.openInputStream(uri);
            }else {
               File file = new File(uri.getEncodedPath());
               len = file.length();
               in = new FileInputStream(file);
            }
            data = new byte[(int)len];
            in.read(data, 0, (int)len);
            contents = new String(data);
         }catch (Exception e) {
            contents = null;
         }
      }
      return contents;
   }

   /*---------------------------------------------------------------interpret-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public native int interpret(
      String script,
      RexxConsole console,
      String baseUri,
      Speaker speaker
   );
}
/*===========================================================================*/
