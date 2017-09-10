package simonaddicott.dev.freespeech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity {

    SharedPreferences prefs = null;
    EditText username;

    String api = "http://api.freespeech.gq/data.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("com.mycompany.myAppName", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        File id = new File("freespeech-id");

        if (id.exists()) {

            // display home page

        } else {

            displayWelcomePage();

        }
    }

    public Boolean writeToFile(String fileName, String bodyToWrite) {
        try {
            FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(bodyToWrite.getBytes());
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String readFromFile(String fileName) {
        String returnContent = "";
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(openFileInput(fileName)));
            String inputString;
            StringBuffer stringBuffer = new StringBuffer();
            while ((inputString = inputReader.readLine()) != null) {
                stringBuffer.append(inputString);
            }
            returnContent = stringBuffer.toString();
            return returnContent;
        } catch (IOException e) {
            return null;
        }
    }

    private void displayWelcomePage() {
        // display welcome menu
        LinearLayout layout = (LinearLayout) findViewById(R.id.activity_main);
        layout.setBackgroundColor(getResources().getColor(R.color.black));

        TextView head = new TextView(this);
        head.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        head.setTextSize(getResources().getDimension(R.dimen.headTextSize));
        head.setTextColor(getResources().getColor(R.color.white));
        head.setText("Welcome");

        TextView subHead = new TextView(this);
        subHead.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        subHead.setTextSize(getResources().getDimension(R.dimen.subTextSize));
        subHead.setTextColor(getResources().getColor(R.color.grey));
        subHead.setText("Please enter a username to begin");

        username = new EditText(this);

        LinearLayout.LayoutParams usernameParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        usernameParam.setMargins(0, 100, 0, 100);
        username.setLayoutParams(usernameParam);
        username.setTextColor(getResources().getColor(R.color.white));
        username.setHint("Username");
        username.setHintTextColor(getResources().getColor(R.color.grey));

        Button submit = new Button(this);
        submit.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        submit.setBackgroundColor(getResources().getColor(R.color.darkGrey));
        submit.setTextColor(getResources().getColor(R.color.white));
        submit.setText("Go");

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (username.getText().length() == 0) {
                    // warn
                } else {
                    register(username.getText().toString());
                }

            }
        });

        layout.addView(head);
        layout.addView(subHead);
        layout.addView(username);
        layout.addView(submit);
    }

    private boolean register(String username) {
        // internalKeyPair used for comms with server
        KeyPair internalKeyPair = getKeyPair();

        // externalKeyPair used for comms with contacts
        KeyPair externalKeyPair = getKeyPair();
        // We'll use this later on

        // Store the private key - using temp unsecure storage for now
        writeToFile("self-serv-private", internalKeyPair.getPrivate().toString() ) ;

        // Send of the request
        RegisterRequest request = new RegisterRequest();
        request.execute( username, internalKeyPair.getPublic().toString()) ;

        return true;
    }

    public static KeyPair getKeyPair() {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            kp = kpg.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return kp;
    }

    public class RegisterRequest extends AsyncTask<String, Void, String> {


        private String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (AbstractMap.SimpleEntry pair : params) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(pair.getKey().toString(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue().toString(), "UTF-8"));
            }

            return result.toString();
        }

        @Override
        protected String doInBackground(String... params) {
            String stringUrl = api;
            String result;

            try {

                URL myUrl = new URL(stringUrl);
                HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);

                String username = params[0];
                String publicKey = params[1];

                String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

                List<AbstractMap.SimpleEntry> paramms = new ArrayList<AbstractMap.SimpleEntry>();

                paramms.add( new AbstractMap.SimpleEntry( "action", "register" ));
                paramms.add( new AbstractMap.SimpleEntry( "username", username ) );
                paramms.add( new AbstractMap.SimpleEntry( "public-key", publicKey ) );
                paramms.add( new AbstractMap.SimpleEntry( "uuid", android_id ) );
                paramms.add( new AbstractMap.SimpleEntry( "device", "android" ));

                OutputStream os = connection.getOutputStream();

                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                writer.write(getQuery(paramms));

                writer.flush();
                writer.close();
                os.close();

                //connection.setReadTimeout(6000);
                //connection.setConnectTimeout(60);

                connection.connect();

                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();

                String line;
                do {
                    line = reader.readLine();
                    stringBuilder.append(line);
                } while (line != null);

                reader.close();
                streamReader.close();

                result = stringBuilder.toString();
                return result;
            } catch (IOException e) {
                e.printStackTrace();
                result = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result.length() > 1 ) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if( jsonObject.getString( "request" ) == "ok" ){

                        Toast.makeText( getApplicationContext(), "Device registered", Toast.LENGTH_LONG );
                        JSONObject data = jsonObject.getJSONObject(" data" );
                        writeToFile( "freespeech-id", data.getString("new-id") );

                        // load home page

                    } else if( jsonObject.getString("request") == "failed" ){

                        JSONObject data = jsonObject.getJSONObject(" data" );
                        Toast.makeText( getApplicationContext(), data.getString("reason"), Toast.LENGTH_LONG );

                    } else {

                        Toast.makeText( getApplicationContext(), "UNKNOWN:" + result, Toast.LENGTH_LONG );

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
