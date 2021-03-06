/*
* (C) Copyright 2011-2014 Jaxo Inc.  All rights reserved.
* This work contains confidential trade secrets of Jaxo.
* Use, examination, copying, transfer and disclosure to others
* are prohibited, except with the express written agreement of Jaxo.
*
* Author:  Pierre G. Richard
* Written: 08/05/2011
* Migrated from ScriptsList.java
*/
package com.jaxo.android.rexx;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ViewAnimator;

/*-- class Landing --+
*//**
* @author  Pierre G. Richard
*/
public class Landing extends ListActivity
{
   private static final int NEW_SCRIPT_REQCODE = 0;
   private static final int EDIT_SCRIPT_REQCODE = 1;
   private static final int IMPORT_FILE_REQCODE = 2;

   private static final int PREFERENCES_ID = Menu.FIRST;
   private static final int NEW_ID = Menu.FIRST + 1;
   private static final int INFO_ID = Menu.FIRST + 2;
   private static final int EDIT_ID = Menu.FIRST + 3;
   private static final int RUN_ID = Menu.FIRST + 4;
   private static final int DELETE_ID = Menu.FIRST + 5;
   private static final int IMPORT_ID = Menu.FIRST + 6;

   private static final String TAG = "Landing";

   private RexxDatabase m_rexxDb;
   private Speaker m_speaker;  // just to keep the service alive

   @Override
   /*----------------------------------------------------------------onCreate-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      Log.i(TAG, "onCreate");
      setTitle(R.string.RexxGallery);
      setContentView(R.layout.landing);
      new AsyncTask<Void, Void, Void>() {
         @Override
         protected Void doInBackground(Void... params) {
            m_speaker = new Speaker(Landing.this);
            m_rexxDb = new RexxDatabase(Landing.this);
            registerForContextMenu(getListView());
            return null;
         }
         @Override
         protected void onPostExecute(Void v) {
            refreshList();
            new Handler().post(
               new Runnable() {
                  @Override
                  public void run() {
                    ((ViewAnimator)findViewById(R.id.viewanimator)).showNext();
                  }
               }
            );
         }
      }.execute();
   }

   @Override
   /*---------------------------------------------------------------onDestroy-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public void onDestroy() {
      super.onDestroy();
      Log.i(TAG, "onDestroy");
      m_speaker.close();
      m_rexxDb.close();
   }

   @Override
   /*----------------------------------------------------------------onResume-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   protected void onResume() {
      super.onResume();
      ensureTextSize();
   }

   //@Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); Log.i(TAG, "onCreate"); }
   //@Override protected void onStart() { super.onStart(); Log.i(TAG, "onStart"); }
   //@Override protected void onResume() { super.onResume(); Log.i(TAG, "onResume"); }
   //@Override protected void onPause() { super.onPause(); Log.i(TAG, "onPause"); }
   //@Override protected void onStop() { super.onStop(); Log.i(TAG, "onStop"); }
   //@Override protected void onDestroy() { super.onDestroy(); Log.i(TAG, "onDestroy"); }

   @Override
   /*-----------------------------------------------------onCreateOptionsMenu-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public boolean onCreateOptionsMenu(Menu menu) {
      super.onCreateOptionsMenu(menu);
      menu.add(Menu.NONE, NEW_ID, Menu.NONE, R.string.NewScript).
      setIcon(android.R.drawable.ic_menu_add);
      menu.add(Menu.NONE, IMPORT_ID, Menu.NONE, R.string.Import).
      setIcon(android.R.drawable.ic_input_get);
      menu.add(0, PREFERENCES_ID, 0, R.string.Preferences).
      setIcon(android.R.drawable.ic_menu_preferences);
      menu.add(Menu.NONE, INFO_ID, Menu.NONE, R.string.Info).
      setIcon(android.R.drawable.ic_menu_info_details);
      return true;
   }

   @Override
   /*------------------------------------------------------onMenuItemSelected-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public boolean onMenuItemSelected(int featureId, MenuItem item)
   {
      switch (item.getItemId()) {
      case PREFERENCES_ID:
         startActivity(new Intent(this, Preferences.class));
         return true;
      case NEW_ID:
         createScript();
         return true;
      case IMPORT_ID:
         startActivityForResult(
            new Intent(this, FileChooser.class),
            IMPORT_FILE_REQCODE
         );
         return true;
      case INFO_ID:
         startActivity(new Intent(this, About.class));
         return true;
      }
      return super.onMenuItemSelected(featureId, item);
   }

   /*-----------------------------------------------------onCreateContextMenu-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   @Override
   public void onCreateContextMenu(
      ContextMenu menu, View v, ContextMenuInfo menuInfo
   ) {
      super.onCreateContextMenu(menu, v, menuInfo);
      try {
         menu.setHeaderIcon(android.R.drawable.ic_menu_more);
         Cursor c = (Cursor)getListAdapter().getItem(
            ((AdapterView.AdapterContextMenuInfo)menuInfo).position
         );
         menu.setHeaderTitle(
            c.getString(c.getColumnIndex(RexxDatabase.TITLE))
         );
         menu.add(0, EDIT_ID, 0, R.string.EditScript);
         menu.add(0, RUN_ID, 0, R.string.RunScript);
         menu.add(0, DELETE_ID, 0, R.string.DeleteScript);
      }catch (Exception e) {
      }
   }

   @Override
   /*---------------------------------------------------onContextItemSelected-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   public boolean onContextItemSelected(MenuItem item)
   {
      switch (item.getItemId()) {
      case EDIT_ID:
         editScript(((AdapterContextMenuInfo)item.getMenuInfo()).id, false);
         return true;
      case RUN_ID:
         editScript(((AdapterContextMenuInfo)item.getMenuInfo()).id, true);
         return true;
      case DELETE_ID:
         m_rexxDb.deleteScript(
            ((AdapterContextMenuInfo)item.getMenuInfo()).id
         );
         refreshList();
         return true;
      default:
         return super.onContextItemSelected(item);
      }
   }

   /*----------------------------------------------------------ensureTextSize-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private void ensureTextSize() {
      float prefdSizeInPx = Preferences.getTextSizeInPx(this);
      ListView v = (ListView)findViewById(android.R.id.list);
      for (int i=0, max=v.getChildCount(); i < max; ++i) {
         TextView t = (TextView)((LinearLayout)v.getChildAt(i)).getChildAt(1);
         float actualSizeInPx = t.getTextSize(); // pixels
         if (actualSizeInPx != prefdSizeInPx) {
            refreshList();
            break;
         }
      }
   }

   /*-------------------------------------------------------------refreshList-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private void refreshList()
   {
      if (m_rexxDb == null) return;  // defense against unattended refreshList
      final float titleSize = Preferences.getTextSizeInSp(this);
      final float dateSize = titleSize * 0.7f;
      Cursor cursor = m_rexxDb.queryScripts();
      startManagingCursor(cursor);
      SimpleCursorAdapter adapter = new SimpleCursorAdapter(
         this, R.layout.scripts_list_item, cursor,
         new String[] {
            RexxDatabase.TITLE,
            RexxDatabase.UPDATE_DATE,
            RexxDatabase.STATUS
         },
         new int[] {
            R.id.title,
            R.id.date,
            R.id.bullet
         }
      );
      adapter.setViewBinder(
         new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int ix) {
               try {
                  if (ix == cursor.getColumnIndex(RexxDatabase.TITLE)) {
                     ((TextView)view).setTextSize(titleSize);
                     return false;
                  }else if (ix == cursor.getColumnIndex(RexxDatabase.STATUS)) {
                     ((ImageView)view).setImageResource(
                        RexxDatabase.statusToBulletImage(cursor.getInt(ix))
                     );
                     return true;
                  }else if (ix == cursor.getColumnIndex(RexxDatabase.UPDATE_DATE)) {
                     ((TextView)view).setTextSize(dateSize);
                     ((TextView)view).setText(
                        new SimpleDateFormat("yyyy/MM/dd\nHH:mm:ss", Locale.US).
                        format(new Date(cursor.getLong(ix)))
                     );
                     return true;
                  }else {
                     return false;
                  }
               }catch (Exception e) {
                  return false;
               }
            }
         }
      );
      setListAdapter(adapter);
   }

   /*------------------------------------------------------------createScript-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private void createScript() {
      startActivityForResult(
         new Intent(this, ScriptEditor.class),
         NEW_SCRIPT_REQCODE
      );
   }

   /*--------------------------------------------------------------editScript-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   private void editScript(long id, boolean isForImmediateRun) {
      Intent intent = new Intent(this, ScriptEditor.class);
      intent.putExtra(RexxDatabase._ID, id);
      intent.putExtra(ScriptEditor.FOR_IMMEDIATE_RUN, isForImmediateRun);
      startActivityForResult(intent, EDIT_SCRIPT_REQCODE);
   }

   @Override
   /*---------------------------------------------------------onListItemClick-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   protected void onListItemClick(ListView l, View v, int position, long id) {
      super.onListItemClick(l, v, position, id);
      editScript(id, Preferences.isImmediateRun(this));
   }

   @Override
   /*--------------------------------------------------------onActivityResult-+
   *//**
   *//*
   +-------------------------------------------------------------------------*/
   protected void onActivityResult(
      int requestCode, int resultCode, Intent intent
   ) {
      super.onActivityResult(requestCode, resultCode, intent);
      if (m_rexxDb == null) {
         /*
         | Don't rely on m_rexxDb not null: the AsynTask started in onCreate
         | might be not yet finished..  How is that possible? Simply because
         | this activity was destroyed after having called the Rexx intent.
         | As a quick dirty fix, let's just forget about the activity result
         | when the RexxDb has not been set: refreshList will be called
         | as the AsynchTask's post-process. Regarding an ImportFile, I think
         | this case has almost no changes to occur.
         */
         return;
      }
      if ((requestCode == IMPORT_FILE_REQCODE) && (resultCode == RESULT_OK)) {
         try {
            m_rexxDb.importScript(
               new FileReader(new File(intent.getStringExtra("filepath")))
            );
         }catch (IOException e) {
         }
      }
      refreshList();
   }
}
/*===========================================================================*/
