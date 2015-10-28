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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity {

  List<String> phrases = new LinkedList<String>();
  Map<String, List<String>> wordMap = new HashMap<>();

  // Change this in order to change the total number of trials
  int totalTrialNum = 5;
  int currTrialNum = 0;
  float totalErrors = 0.0f;
  int totalEntered = 0;
  int totalExpected = 0;

  String currTarget = "";
  String currEntered = "";

  long startTime, endTime;
  long trialStart, trialFinish;

  int prevAddedLength = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    loadPhrases();
    loadWords();

    // Comment out this line in order to get the same phrases each time (for testing purposes)
    Collections.shuffle(phrases);
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
    int id = v.getId();

    String text;

    if (id == R.id.suggest_button1 || id == R.id.suggest_button2 || id == R.id.suggest_button3) {

      text = (String) ((TextView) findViewById(id)).getText();
      String lastWord = getLastWord();

      if (!(lastWord.length() == 0)) {
        int lastWordStart = currEntered.length() - lastWord.length();
        currEntered = currEntered.substring(0, lastWordStart);
      }

      currEntered += text + " ";

      updateTexts();
      updateSuggestions();

    } else if (id == R.id.delete_key) {

      if (currEntered.length() >= prevAddedLength) {
        currEntered = currEntered.substring(0, currEntered.length() - prevAddedLength);
      }

    } else {

      Button b = (Button) v; // guaranteed in this case

      String key = (String) b.getText();
      currEntered += key;

    }

    updateSuggestions();
    updateTexts();
  }

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

  private void loadPhrases() {
    InputStream raw = getResources().openRawResource(R.raw.phrases2);
    InputStreamReader inputReader = new InputStreamReader(raw);
    BufferedReader buffReader = new BufferedReader(inputReader);

    String line;

    try {
      while ((line = buffReader.readLine()) != null) {
        phrases.add(line);
      }

      buffReader.close();
      inputReader.close();
      raw.close();

    } catch (Exception e) {
      Log.e("MainActivity", "Check that the phrases2.txt file still exists in the raw folder");
    }

  }

  private void loadWords() {
    InputStream raw = getResources().openRawResource(R.raw.count_1w);
    InputStreamReader inputReader = new InputStreamReader(raw);
    BufferedReader buffReader = new BufferedReader(inputReader);

    String line;

    try {
      while ((line = buffReader.readLine()) != null) {
        String[] parsed = line.split("\\s+");
        String word = parsed[0];

        for (int i = 1; i < min(word.length(), 6); i++) {
          if (wordMap.get(word.subSequence(0, i)) != null) {
            if (wordMap.get(word.subSequence(0, i)).size() < 3) {
              List<String> temp = wordMap.get(word.subSequence(0, i));
              temp.add(word);

              wordMap.put((String) word.subSequence(0, i), temp);
            }
          } else {
            List<String> newAdd = new ArrayList<String>();
            newAdd.add(word);
            wordMap.put(word.subSequence(0, i).toString(), newAdd);
          }
        }
      }

      buffReader.close();
      inputReader.close();
      raw.close();

    } catch (Exception e) {
      e.printStackTrace();
      Log.v("MainActivity", "Check that the count_1w.txt still exists in the raw folder!");
    }
  }

  private void updateTexts() {
    TextView target = (TextView) findViewById(R.id.target_text_view);
    TextView entered = (TextView) findViewById(R.id.entered_text_view);

    target.setText("Target: " + currTarget);
    entered.setText("Entered: " + currEntered + "|");
  }

  private void updateSuggestions() {
    List<String> suggestions;
    String last = getLastWord();

    if (wordMap.containsKey(last)) {
      suggestions = wordMap.get(last);
    } else {
      return;
    }

    String suggestion;
    Button suggestButton;

    // sadly there isn't any way to automate this... getting the ID's is
    // reasonlessly difficult
    suggestion = suggestions.get(0);
    suggestButton = (Button) findViewById(R.id.suggest_button1);
    suggestButton.setText(suggestion);

    suggestion = suggestions.get(1);
    suggestButton = (Button) findViewById(R.id.suggest_button2);
    suggestButton.setText(suggestion);

    suggestion = suggestions.get(2);
    suggestButton = (Button) findViewById(R.id.suggest_button3);
    suggestButton.setText(suggestion);
  }

  private String getLastWord() {
    if (currEntered.length() == 0 ||
        currEntered.charAt(currEntered.length() - 1) == ' ') {
      return "";
    }

    String[] words = currEntered.split(" ");
    return (words[words.length - 1]);
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
      line.setText("WPM: " + (totalEntered / 5.0f) / ((endTime - startTime) / 60000f));
      line = (TextView) findViewById(R.id.stat_line7);
      line.setVisibility(View.GONE);
      line = (TextView) findViewById(R.id.stat_line8);
      line.setVisibility(View.GONE);

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
