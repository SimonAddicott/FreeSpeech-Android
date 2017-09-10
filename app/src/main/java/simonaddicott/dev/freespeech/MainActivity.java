package simonaddicott.dev.freespeech;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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

public class MainActivity extends AppCompatActivity {

    SharedPreferences prefs = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("com.mycompany.myAppName", MODE_PRIVATE);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (prefs.getBoolean("firstrun", true)) {
            prefs.edit().putBoolean("firstrun", false).commit();
            // First run
            // todo
            // display welcome
            // obtain username
            // internalKeyPair used for comms with server
            KeyPair internalKeyPair = getKeyPair();
            // externalKeyPair used for comms with contacts
            KeyPair externalKeyPair = getKeyPair();
            // register device


        } else {

        }
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

    public class HttpGetRequest extends AsyncTask<String, Void, String> {

        public Boolean writeToFile(String fileName, String bodyToWrite){
            try {
                FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
                fos.write(bodyToWrite.getBytes());
                fos.close();
                return true;
            } catch(IOException e){
                return false;
            }
        }

        public String readFromFile(String fileName){
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
            } catch(IOException e){
                return null;
            }
        }

        private String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (AbstractMap.SimpleEntry pair : params)
            {
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
            String stringUrl = params[0];
            String result;

            try {
                URL myUrl = new URL(stringUrl);
                HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();
                if( params[1] == "POST" ) {
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    String accountNumber = params[2];
                    String passToken = params[3];

                    String android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

                    List<AbstractMap.SimpleEntry> paramms = new ArrayList<AbstractMap.SimpleEntry>();

                    writeToFile("user", accountNumber);

                    OutputStream os = connection.getOutputStream();

                    BufferedWriter writer = new BufferedWriter( new OutputStreamWriter(os, "UTF-8") );

                    writer.write(getQuery(paramms));

                    writer.flush();
                    writer.close();
                    os.close();
                }

                //connection.setReadTimeout(6000);
                //connection.setConnectTimeout(60);

                connection.connect();

                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();

                String line;
                do{
                    line = reader.readLine();
                    stringBuilder.append(line);
                } while( line != null );

                reader.close();
                streamReader.close();

                result = stringBuilder.toString();
                return result;
            }
            catch(IOException e){
                e.printStackTrace();
                result = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if( result.contains("success") ){
                try {
                    JSONObject jsonObject = new JSONObject(result);
                } catch( JSONException e ){
                    e.printStackTrace();
                }
            }
        }
    }
}
