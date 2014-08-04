/*
* (C) Copyright 2011-2014 Jaxo Inc.  All rights reserved.
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
import android.util.Log;

/*-- class Rexx --+
*//**
* @author  Pierre G. Richard
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
   public static String SCRIPT_ARGS_KEY = "jaxo.rexx.args";        // String
   public static String RESULT_KEY = "jaxo.rexx.result";           // String
   public static String REXX_MESSAGE_KEY = "jaxo.rexx.message";    // String
   public static String REXX_ERRORCODE_KEY = "jaxo.rexx.rexxcode"; // int
   public static int RESULTCODE_OK = RESULT_FIRST_USER;
   public static int RESULTCODE_ERROR = RESULT_FIRST_USER + 1;
   public static int RESULTCODE_EXCEPTION_THROWN = RESULT_FIRST_USER + 2;

   private Speaker m_speaker;
   private RexxConsole m_console;

   @Override
   /*----------------------------------------------------------------onCreate-+
   *//**
   *//*
   * Activity is single-top when started from REXX
   +-------------------------------------------------------------------------*/
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      Log.i("REXX", "onCreate");
      setContentView(R.layout.console);
      setTitle(R.string.RexxInterpreter);
      try {
         URI base = new URI("file://" + getFilesDir().getAbsolutePath() + "/");
         m_speaker = new Speaker(this);
         m_console = new RexxConsole(
            (Activity)Rexx.this, base, (TextView)findViewById(R.id.say_view)
         );
         initialize(m_console, base.toString(), m_speaker);
         new InterpreterThread(getIntent()).start();
      }catch (Exception e) {
         setResult(RESULT_CANCELED);
         finish();
      }
   }

   @Override
   /*-------------------------------------------------------------onNewIntent-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   protected void onNewIntent(Intent intent) {
      super.onNewIntent(intent);
      Log.i("REXX", "onNewIntent: console is " + m_console.toString());
      setIntent(intent);
      new InterpreterThread(intent).start();
   }

   @Override
   /*---------------------------------------------------------------onDestroy-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public void onDestroy() {
      super.onDestroy();
      Log.i("REXX", "onDestroy");
      m_console.flush();
      m_speaker.close();
      finalize();
   }

   /*----------------------------------------------- class InterpreterThread -+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private class InterpreterThread extends Thread {
      String m_content;
      String m_args;
      /*----------------------------------------------------InterpreterThread-+
      *//**
      *//*
      +----------------------------------------------------------------------*/
      InterpreterThread(Intent intent) {
         Bundle extras = intent.getExtras();
         if (extras == null) {
            m_content = getIntentData(intent);             // implicit
            m_args = "";
         }else {
            if (!extras.containsKey(SCRIPT_CONTENT_KEY)) {
               m_content = getIntentData(intent);          // implicit
            }else {
               m_content = extras.getString(SCRIPT_CONTENT_KEY);
            }
            if (extras.containsKey(SCRIPT_ARGS_KEY)) {
               m_args = extras.getString(SCRIPT_ARGS_KEY);
            }else {
               m_args = "";
            }
         }
         if (m_content == null) throw new IllegalArgumentException();
      }
      @Override
      /*------------------------------------------------------------------run-+
      *//**
      *//*
      +----------------------------------------------------------------------*/
      public void run() {
         int rc = interpret(m_content, m_args);
         m_console.flush();
         int resultCode;
         Intent intent = new Intent();
         intent.putExtra(RESULT_KEY, m_console.m_result);
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
         if (rc != 0) finish(); // otherwise, wait so the user can read!
      }
   }

   /*-----------------------------------------------------------getIntentData-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private String getIntentData(Intent intent) {
      String content = null;
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
            content = new String(data);
         }catch (Exception e) {
            content = null;
         }
      }
      return content;
   }

   /*--------------------------------------------------------------- Natives -+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   // this field is meant to be accessed by the  JNI side
   private long context;

   public native void initialize(RexxConsole console, String baseUri, Speaker speaker);
   public native int interpret(String script, String args);
   public native void finalize();
}
/*===========================================================================*/
