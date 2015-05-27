package appguarden.apppal_checker;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import apppal.logic.evaluation.AC;
import apppal.logic.evaluation.Evaluation;
import apppal.logic.evaluation.Result;
import apppal.logic.language.Assertion;


public class MainActivity extends ActionBarActivity
{
  static public Context instance = null;
  AC ac = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    instance = this;

    setDefaultPolicy(getCurrentFocus());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings)
    {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void setDefaultPolicy(View view)
  {
    try
    {
      this.ac = new AC("\"user\" says App isInstallable if App isAnApp.");
    } catch (IOException e)
    {
      Log.e("AppPAL", "failed to create AC");
    }
    this.populateApps(view);
  }

  public void setFalsePolicy(View view)
  {
    try
    {
      this.ac = new AC("\"user\" says App isInstallable if App isAnApp where true = false.");
    } catch (IOException e)
    {
      Log.e("AppPAL", "failed to create AC");
    }
    this.populateApps(view);
  }

  public void setPermPolicy(View view)
  {
    try
    {
      this.ac = new AC("\"user\" says App isInstallable if App isAnApp where hasPermission(App, \"android.permission.ACCESS_FINE_LOCATION\") = true.");
    } catch (IOException e)
    {
      Log.e("AppPAL", "failed to create AC");
    }
    this.populateApps(view);
  }


  // Create the list of apps to be displayed
  public void populateApps(View view)
  {
    final PackageManager pm = getPackageManager();
    final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

    final ArrayList<String> names = new ArrayList<>();
    for (ApplicationInfo packageInfo : packages)
    {
      final String name = packageInfo.packageName;
      // Don't worry about Google stuff
      if ((name.startsWith("com.google.") ||
        name.startsWith("com.android.") ||
        name.equals("android")))
        continue;
      names.add(name);
    }

    final Map<String, Boolean> results = checkApps(names);
    final ArrayList<String> output = new ArrayList<>();
    for (Map.Entry<String, Boolean> pair : results.entrySet())
      output.add((pair.getValue() ? "✅" : "❌") + " " + pair.getKey());

    final ArrayAdapter<String> adapter =
      new ArrayAdapter<>(
        this,
        android.R.layout.simple_list_item_1,
        output);

    final ListView listView = (ListView) findViewById(R.id.listApps);
    listView.setAdapter(adapter);
  }

  // Check apps against a policy
  private Map<String, Boolean> checkApps(List<String> names)
  {
    final Map<String, Boolean> results = new ArrayMap<>();
    for (final String name : names)
    {
      try
      {
        final Evaluation eval = new Evaluation(this.ac);
        final String query = "\"user\" says \"apk://" + name + "\" isInstallable.";
        final Result result = eval.run(Assertion.parse(query));
        results.put(name, result.isProven());
      } catch (IOException e)
      {
        Log.e("AppPAL", "Couldn't evaluate query about: " + name);
        results.put(name, Boolean.FALSE);
      }
    }

    return results;
  }
}
