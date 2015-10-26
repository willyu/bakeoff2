package fall2015.dhcs.minitype;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity {

  List<String> phrases = new LinkedList<String>();

  // Change this in order to change the total number of trials
  int totalTrialNum = 2;
  int currTrialNum = 0;
  float totalErrors = 0.0f;
  int totalEntered = 0;
  int totalExpected = 0;

  String currTarget = "";
  String currEntered = "";

  long startTime, endTime;
  long trialStart, trialFinish;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    InputStream raw = getResources().openRawResource(R.raw.phrases2);
    InputStreamReader inputReader = new InputStreamReader(raw);
    BufferedReader buffReader = new BufferedReader(inputReader);

    String line;

    try {
      while ((line = buffReader.readLine()) != null) {
        phrases.add(line);
      }
    } catch (Exception e) {
      Log.e("MainActivity", "Check that the phrases2.txt file still exists in the raw folder");
    }

    // Comment out this line in order to get the same phrases each time (for testing purposes)
//    Collections.shuffle(phrases);
    currTarget = phrases.get(currTrialNum);

  }

  public void startTrials(View v) {
    ((Button) findViewById(R.id.start_button)).setEnabled(false);
    findViewById(R.id.start_time_text_view).setVisibility(View.INVISIBLE);

    findViewById(R.id.target_text_view).setVisibility(View.VISIBLE);
    findViewById(R.id.entered_text_view).setVisibility(View.VISIBLE);
    findViewById(R.id.next_button).setVisibility(View.VISIBLE);
    findViewById(R.id.input_window).setVisibility(View.VISIBLE);
    findViewById(R.id.statistics_header).setVisibility(View.VISIBLE);

    startTime = System.currentTimeMillis();
    trialStart = startTime;

    updateTexts();
  }

  public void keystroke(View v) {


    if (((View) v).getId() == R.id.delete_key) {

      if (currEntered.length() > 0) {
        currEntered = currEntered.substring(0, currEntered.length() - 1);
      }

    } else {

      Button b = (Button) v; // guaranteed in this case

      String key = (String) b.getText();
      currEntered += key;
      
    }

    updateTexts();
  }

  //TODO: change to display statistics, as well as update the internal statistics
  public void nextTrial(View v) {

    if (currTrialNum >= totalTrialNum) {
      return;
    }

    boolean finished = (currTrialNum == totalTrialNum - 1);
    if (finished) endTime = System.currentTimeMillis();

    float errors = computeLevenshteinDistance(currEntered.trim(), currTarget.trim());

    totalErrors += errors;
    totalEntered += currEntered.length();
    totalExpected += currTarget.length();

    trialFinish = System.currentTimeMillis();

    displayStats(finished);

    if (finished) {
      findViewById(R.id.input_window).setVisibility(View.GONE);
      ((Button) findViewById(R.id.next_button)).setEnabled(false);
      findViewById(R.id.next_button).setVisibility(View.GONE);
      findViewById(R.id.finished).setVisibility(View.VISIBLE);
      findViewById(R.id.target_text_view).setVisibility(View.GONE);
      findViewById(R.id.entered_text_view).setVisibility(View.GONE);

      RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.statistics_header).getLayoutParams();
      params.addRule(RelativeLayout.BELOW, R.id.finished);
    } else {
      trialStart = System.currentTimeMillis();
      currTrialNum += 1;
      currTarget = phrases.get(currTrialNum);
      currEntered = "";
      updateTexts();
    }

  }

  private void updateTexts() {
    TextView target = (TextView) findViewById(R.id.target_text_view);
    TextView entered = (TextView) findViewById(R.id.entered_text_view);

    target.setText("Target: " + currTarget);
    entered.setText("Entered: " + currEntered + "|");
  }

  private void displayStats(boolean finished) {

    TextView line;

    if (!finished) {
      line = (TextView) findViewById(R.id.stat_line1);
      line.setText("Phrase " + (currTrialNum + 1) + " of " + totalTrialNum);
      line = (TextView) findViewById(R.id.stat_line2);
      line.setText("Target phrase: " + currTarget);
      line = (TextView) findViewById(R.id.stat_line3);
      line.setText("Phrase length: " + currTarget.length());
      line = (TextView) findViewById(R.id.stat_line4);
      line.setText("User typed: " + currEntered);
      line = (TextView) findViewById(R.id.stat_line5);
      line.setText("User typed length: " + currEntered.length());
      line = (TextView) findViewById(R.id.stat_line6);
      line.setText("Number of errors: " + computeLevenshteinDistance(currEntered.trim(), currTarget.trim()));
      line = (TextView) findViewById(R.id.stat_line7);
      line.setText("Time taken on this trial: " + (trialFinish - trialStart));
      line = (TextView) findViewById(R.id.stat_line8);
      line.setText("Time taken since the beginning: " + (trialFinish - startTime));
    } else {
      line = (TextView) findViewById(R.id.stat_line1);
      line.setText("Trials complete!");
      line = (TextView) findViewById(R.id.stat_line2);
      line.setText("Total time taken: " + (endTime - startTime));
      line = (TextView) findViewById(R.id.stat_line3);
      line.setText("Total letters entered: " + totalEntered);
      line = (TextView) findViewById(R.id.stat_line4);
      line.setText("Total letters expected: " + totalExpected);
      line = (TextView) findViewById(R.id.stat_line5);
      line.setText("Total errors entered: " + totalErrors);
      line = (TextView) findViewById(R.id.stat_line6);
      line.setText("WPM: " + (totalEntered / 5.0f)/((endTime - startTime)/60000f));
    }

  }

  /**
   * Computes the Levenshtein distance between two strings (copied from scaffold code)
   *
   * @param s1 first string
   * @param s2 second string
   * @return the Levenshtein distance
   */
  private int computeLevenshteinDistance(String s1, String s2) {
    int[][] distance = new int[s1.length() + 1][s2.length() + 1];

    for (int i = 0; i <= s1.length(); i++) {
      distance[i][0] = i;
    }
    for (int j = 1; j <= s2.length(); j++) {
      distance[0][j] = j;
    }

    for (int i = 1; i <= s1.length(); i++) {
      for (int j = 1; j <= s2.length(); j++) {
        distance[i][j] = min(min(distance[i - 1][j] + 1, distance[i][j - 1] + 1), distance[i - 1][j - 1] + ((s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1));
      }
    }

    return distance[s1.length()][s2.length()];
  }

}
