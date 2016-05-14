package com.example.android.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArrayAdapter<String> mForecastAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Criar alguns dados fictícios para o ListView. Aqui está uma previsão semanal
           String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));

        ListView view = (ListView)findViewById(R.id.listView_forecast);
        mForecastAdapter = new ArrayAdapter<String>(this, R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);
        view.setAdapter(mForecastAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forecast, menu);
        inflater.inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("indaiatuba");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void , String[]>
    {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            // Esses dois precisam ser declarados fora do try/catch
            // para que possam ser fechados no bloco do finally.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            int numDays = 7;
            try {
                // Construir a URL para a consulta no OpenWeatherMap
                // Os parâmetros possíveis estão disponíveis na página de previsão do API do OpenWeatherMap, pelo
                // http://openweathermap.org/API#forecast.
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "ctn";
                final String KEY_PARAM = "appid";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(KEY_PARAM, "532d313d6a9ec4ea93eb89696983e369")
                        .build();


                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI: " + builtUri.toString());

                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=indaiatuba&mode=json&units=metric&cnt=7&appid=532d313d6a9ec4ea93eb89696983e369");


                // Criar o pedido para OpenWeatherMap, e abrir a conexão.
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Ler o ImputStream para uma String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nada a fazer.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Como é JSON, acrescentar uma nova linha não é necessário (isso não afetará a análise)
                    // Mas faz a depuração ficar mais fácil se você imprimir.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream estava vazio. Nada para análisar.
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // Se o código não pegar os dados corretamente, não há o que análisar.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null){
                mForecastAdapter.clear();
                mForecastAdapter.addAll(result);
            }
        }
    }

    private String getReadableDateString(long time){
        // Como a API retorna um timestamp unix (medido em segundos),
        // Deve ser convertido em milissegundos, a fim de ser convertido em uma data válida.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepara os dados da mínima e máxima
     */
    private String formatHighLows(double high, double low) {
        // Para a apresentação, suponha que o usuário não se preocupa com décimos de grau.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    /**
      * Pegue a String representando a previsão completa em formato JSON e
      * Retiree os dados que precisamos.
      *
      * Felizmente análise é fácil: construtor recebe a string JSON e converte-lo
      * Em uma hierarquia de objetos para nós.
      */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // Estes são os nomes dos objetos JSON que precisam de ser extraído.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OEM retorna previsões diárias com base no horário local da cidade que está sendo
        // pedida, o que significa que precisamos saber o desvio GMT de traduzir esses dados
        // corretamente.

        // Uma vez que esses dados também são enviados em ordem e o
        // primeiro dia é sempre o dia atual, vamos tirar proveito disso para obter uma
        // data UTC normalizada agradável para todo o nosso tempo.

        Time dayTime = new Time();
        dayTime.setToNow();

        // Nós começamos no dia retornado pela hora local. Caso contrário, vira uma bagunça.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // agora nós trabalhos apenas no UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // Por enquanto, o formato será: "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Pega o objeto JSON representando o dia
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // A data/hora é retornada como long.
            // Precisamos converter isso em algo legível,
            // uma vez que a maioria das pessoas não vai ler "1400356800" como "neste sábado".
            long dateTime;
            // Trapacear para converter isso em tempo UTC, que é o que nós queremos de qualquer maneira
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // descrição é um array filho chamado "weather", que é um único elemento longo.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // As temperatura são um filho do ojeto chamado "temp".
            // Tente não chamar variaveis de "temp" quando trabalhar com temperaturas. Confunde todo mundo.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;
    }
}
