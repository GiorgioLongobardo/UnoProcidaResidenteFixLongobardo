package com.porfirio.orariprocida2011.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.porfirio.orariprocida2011.R;
import com.porfirio.orariprocida2011.adapter.MezzoAdapter;
import com.porfirio.orariprocida2011.dialogs.DettagliMezzoDialog;
import com.porfirio.orariprocida2011.dialogs.WeatherDialog;
import com.porfirio.orariprocida2011.entity.Alert;
import com.porfirio.orariprocida2011.entity.Compagnia;
import com.porfirio.orariprocida2011.entity.Meteo;
import com.porfirio.orariprocida2011.entity.Mezzo;
import com.porfirio.orariprocida2011.entity.Osservazione;
import com.porfirio.orariprocida2011.threads.alerts.AlertUpdate;
import com.porfirio.orariprocida2011.threads.alerts.OnRequestAlertsDAO;
import com.porfirio.orariprocida2011.threads.companies.CompaniesUpdate;
import com.porfirio.orariprocida2011.threads.companies.OnRequestCompaniesDAO;
import com.porfirio.orariprocida2011.threads.taxies.OnRequestTaxisDAO;
import com.porfirio.orariprocida2011.threads.transports.OnRequestTransportsDAO;
import com.porfirio.orariprocida2011.threads.transports.TransportsUpdate;
import com.porfirio.orariprocida2011.threads.weather.OnRequestWeatherDAO;
import com.porfirio.orariprocida2011.threads.weather.WeatherUpdate;
import com.porfirio.orariprocida2011.utils.Analytics;
import com.porfirio.orariprocida2011.utils.AnalyticsApplication;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class OrariProcida2011Activity extends FragmentActivity {

    private static final String ANALYTICS_CATEGORY_APP_EVENT = "App Event";
    private static final String ANALYTICS_CATEGORY_UI_EVENT = "UI Event";
    private static final String ANALYTICS_CATEGORY_USER_EVENT = "User";

    private static FragmentManager fm;

    public Calendar c;
    public AlertDialog aboutDialog;
    public Meteo meteo;

    public ArrayList<Mezzo> transportList;
    private String[] ragioni = new String[100];

    private String nave;
    private String portoPartenza;
    private String portoArrivo;
    private MezzoAdapter aalvMezzi;
    private List<Mezzo> selectMezzi;
    private DettagliMezzoDialog dettagliMezzoDialog;
    private final ArrayList<Compagnia> listCompagnia = new ArrayList<>();
    private LocationManager myManager;
    private String BestProvider;
    private FloatingActionButton weatherFab;

    private OnRequestCompaniesDAO companiesDAO;
    private OnRequestWeatherDAO weatherDAO;
    private OnRequestTransportsDAO transportsDAO;
    private OnRequestAlertsDAO alertsDAO;
    private OnRequestTaxisDAO taxisDAO;
    private Analytics analytics;

    private boolean hasReceivedWeather, hasReceivedCompanies, hasReceivedTransports, hasReceivedAlerts;

    private ImageButton timeButton, dateButton;
    private Button dateTimeChip;
    private SwipeRefreshLayout swipe_refresh_layout;
    private LottieAnimationView lottieLoader;
    private ImageView blurredBackground;
    private boolean isTimePicked = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // NOTE (2025-02-02):
        // this check breaks the reportFullyDrawn method somehow and the time never shows up in the logs
        // you can uncomment this if you don't intend to use it i guess idk the permission request shouldn't even be here
//        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            analytics.send(ANALYTICS_CATEGORY_APP_EVENT, "Request Permission");
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 534534);
//        }


        ImageView imageView = findViewById(R.id.info_icon);
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(OrariProcida2011Activity.this, InfoActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_center, R.anim.exit_to_center);
        });

        analytics = new Analytics((AnalyticsApplication) getApplication());

        weatherDAO = new OnRequestWeatherDAO();
        weatherDAO.getUpdates().observe(this, this::onWeatherUpdate);
        weatherDAO.requestUpdate();

        transportsDAO = new OnRequestTransportsDAO();
        transportsDAO.getUpdates().observe(this, this::onTransportsUpdate);
        transportsDAO.requestUpdate();

        alertsDAO = new OnRequestAlertsDAO();
        alertsDAO.getUpdates().observe(this, this::onAlertsUpdate);

        companiesDAO = new OnRequestCompaniesDAO();
        companiesDAO.getUpdates().observe(this, this::onCompaniesUpdate);
        companiesDAO.requestUpdate();

        alertsDAO = new OnRequestAlertsDAO();
        alertsDAO.getUpdates().observe(this, this::onAlertsUpdate);
        alertsDAO.requestUpdate();

        taxisDAO = new OnRequestTaxisDAO();
        taxisDAO.requestUpdate();

        fm = getSupportFragmentManager();
        myManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        BestProvider = myManager.getBestProvider(criteria, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.disclaimer) + "\n" + getString(R.string.credits))
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> dialog.cancel());
        aboutDialog = builder.create();


        ragioni = getResources().getStringArray(R.array.strRagioni);

        meteo = new Meteo();

        timeButton = findViewById(R.id.time_button);
        dateButton = findViewById(R.id.date_button);
        dateTimeChip = findViewById(R.id.timeResetButton);
        weatherFab = findViewById(R.id.fabWeather);

        weatherFab.setOnClickListener(v -> {
            List<Osservazione> observations = meteo.getForecasts();
            if (!observations.isEmpty()) {
                Osservazione lastObservation = observations.get(observations.size() - 1);
                String windDirection = getWindDirectionString(lastObservation.getWindDirection());
                String dateTime = lastObservation.getTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
                String windInfo = getWindBeaufortString(lastObservation) + " da " + windDirection + " (" + (int) Math.floor(lastObservation.getWindSpeed()) + "Km/h)";

                WeatherDialog weatherDialog = new WeatherDialog(this,
                        dateTime,
                        windInfo,
                        meteo
                );
                weatherDialog.show();
            }
        });


        blurredBackground = findViewById(R.id.blurredBackground);

        lottieLoader = findViewById(R.id.lottieLoader);
        InputStream inputStream = getResources().openRawResource(R.raw.loading_lottie);
        String jsonString;
        jsonString = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .reduce("", (accumulator, actual) -> accumulator + actual);

        lottieLoader.setAnimationFromJson(jsonString, "loading_animation");


        c = Calendar.getInstance(TimeZone.getDefault());


        timeButton.setOnClickListener(v -> {

            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    OrariProcida2011Activity.this,
                    R.style.TimePickerTheme,
                    (view, hourOfDay, minute1) -> {
                        Calendar currentCalendar = Calendar.getInstance();

                        Calendar selectedTime = (Calendar) c.clone();
                        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedTime.set(Calendar.MINUTE, minute1);
                        selectedTime.set(Calendar.SECOND, 0);
                        selectedTime.set(Calendar.MILLISECOND, 0);

                        if (selectedTime.before(currentCalendar)) {
                            selectedTime.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        c.setTime(selectedTime.getTime());

                        dateTimeChip.setText(String.format("%02d/%02d/%04d - %02d:%02d", selectedTime.get(Calendar.DAY_OF_MONTH),
                                selectedTime.get(Calendar.MONTH) + 1,
                                selectedTime.get(Calendar.YEAR), hourOfDay, minute1));
                        dateTimeChip.setVisibility(VISIBLE);
                        isTimePicked = true;

                        aggiornaLista();
                    },
                    hour, minute, true);

            timePickerDialog.show();
        });

        dateButton.setOnClickListener(v -> {
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);

            // Mostra il DatePickerDialog
            @SuppressLint("DefaultLocale") DatePickerDialog datePickerDialog = new DatePickerDialog(
                    OrariProcida2011Activity.this,
                    R.style.DatePickerTheme,
                    (view, year1, monthOfYear, dayOfMonth1) -> {
                        // Impedire la selezione di date antecedenti alla data attuale
                        Calendar currentCalendar = Calendar.getInstance();
                        if (year1 < currentCalendar.get(Calendar.YEAR) ||
                                (year1 == currentCalendar.get(Calendar.YEAR) && monthOfYear < currentCalendar.get(Calendar.MONTH)) ||
                                (year1 == currentCalendar.get(Calendar.YEAR) && monthOfYear == currentCalendar.get(Calendar.MONTH) && dayOfMonth1 < currentCalendar.get(Calendar.DAY_OF_MONTH))) {
                            year1 = currentCalendar.get(Calendar.YEAR);
                            monthOfYear = currentCalendar.get(Calendar.MONTH);
                            dayOfMonth1 = currentCalendar.get(Calendar.DAY_OF_MONTH);
                        }

                        // Imposta la data scelta
                        c.set(Calendar.YEAR, year1);
                        c.set(Calendar.MONTH, monthOfYear);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth1);

                        //se si sceglie la data di oggi si imposta l'orario a quello attuale così da non mostrare corse antecedenti
                        if (year1 == currentCalendar.get(Calendar.YEAR) && monthOfYear == currentCalendar.get(Calendar.MONTH) && dayOfMonth1 == currentCalendar.get(Calendar.DAY_OF_MONTH)) {
                            Log.d("selectedDate", "entrato 1");
                            c.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY));
                            c.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE));
                            c.set(Calendar.SECOND, 0);
                            c.set(Calendar.MILLISECOND, 0);
                            dateTimeChip.setText(String.format("%02d/%02d/%04d - %02d:%02d", dayOfMonth1, monthOfYear + 1, year1, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE)));
                        } else {
                            // Se l'orario non è stato ancora scelto, imposta a mezzanotte
                            if (!isTimePicked) {
                                Log.d("selectedDate", "entrato 2");
                                c.set(Calendar.HOUR_OF_DAY, 0);
                                c.set(Calendar.MINUTE, 0);
                                c.set(Calendar.SECOND, 0);
                                c.set(Calendar.MILLISECOND, 0);
                            }
                            dateTimeChip.setText(String.format("%02d/%02d/%04d - %02d:%02d", dayOfMonth1, monthOfYear + 1, year1, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
                        }
                        dateTimeChip.setVisibility(VISIBLE);
                        aggiornaLista();
                    },
                    year, month, dayOfMonth);

            datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
            datePickerDialog.show();
        });


        dateTimeChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTime();
            }
        });


        swipe_refresh_layout = findViewById(R.id.swipe_refresh_layout);
        swipe_refresh_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                analytics.send(ANALYTICS_CATEGORY_UI_EVENT, "Update Orari da Web da Menu");

                lottieLoader.setVisibility(VISIBLE);
                lottieLoader.playAnimation();

                blurredBackground.setVisibility(VISIBLE);
                transportsDAO.requestUpdate();

                swipe_refresh_layout.setRefreshing(false);
            }
        });

        transportList = new ArrayList<>();
        GridView lvMezzi = findViewById(R.id.listMezzi);
        selectMezzi = new ArrayList<>();
        aalvMezzi = new MezzoAdapter(this, selectMezzi, c);
        lvMezzi.setAdapter(aalvMezzi);

        dettagliMezzoDialog = new DettagliMezzoDialog(alertsDAO, taxisDAO);
        dettagliMezzoDialog.setDettagliMezzoDialog(fm, this, this, c, meteo);
        dettagliMezzoDialog.setAnalytics(analytics);


        lvMezzi.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
            analytics.send(ANALYTICS_CATEGORY_UI_EVENT, "Click Dettagli Mezzo");
            dettagliMezzoDialog.setMezzo(selectMezzi.get(arg2));
            dettagliMezzoDialog.setListCompagnia(listCompagnia);
            dettagliMezzoDialog.show(fm, "fragment_edit_name");

        });

        dettagliMezzoDialog.setOnReportListener(() -> {
            analytics.send(ANALYTICS_CATEGORY_UI_EVENT, "Update Orari da Web da Menu");

            lottieLoader.setVisibility(VISIBLE);
            lottieLoader.playAnimation();

            blurredBackground.setVisibility(VISIBLE);
            transportsDAO.requestUpdate();
        });


        lvMezzi.setLongClickable(true);
        lvMezzi.setOnItemLongClickListener((arg0, arg1, arg2, arg3) -> {
            analytics.send(ANALYTICS_CATEGORY_UI_EVENT, "LongClick DettagliMezzo");

            if (!isOnline())
                Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.soloOnline), Toast.LENGTH_SHORT).show();
            else {
                dettagliMezzoDialog.setMezzo(selectMezzi.get(arg2));
                dettagliMezzoDialog.setListCompagnia(listCompagnia);
                dettagliMezzoDialog.setReportShortcut(true);
                dettagliMezzoDialog.show(fm, "fragment_edit_name");
                aggiornaLista();
            }
            return true;
        });

        lvMezzi.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    View firstVisibleChild = view.getChildAt(0);
                    if (firstVisibleChild != null) {
                        int top = firstVisibleChild.getTop();
                        swipe_refresh_layout.setEnabled(top >= 0);
                    }
                } else {
                    swipe_refresh_layout.setEnabled(false);
                }
            }
        });


        //aggiungere onlongclick su lvMezzi che faccia partire il dialog di segnalazione
        //che ha due funzioni: segnala un cambiamento (interazione con mail)
        //esegui un cambiamento (richiede una password che conosco solo io)
        //il cambiamento si ottiene andando ad aggiornare un file esclusioni giornaliere
        //ad ogni cambiamento si apre il file, si riscrivono le righe di oggi, si aggiunge la riga della segnalazione
        //bisogna cambiare anche la lettura dei mezzi prevedendo la lettura di questo file con la conseguente
        //eliminazione delle corse indicate

        setSpinner();

        aggiornaLista();
        if (!portoPartenza.equals(getString(R.string.qualsiasi_porto))) {
            showSnackBar(getString(R.string.secondoMeVuoiPartireDa) + " " + portoPartenza);
        }

    }

    private void resetTime() {
        Calendar currentCalendar = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE));
        c.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR));
        c.set(Calendar.MONTH, currentCalendar.get(Calendar.MONTH));
        c.set(Calendar.DAY_OF_MONTH, currentCalendar.get(Calendar.DAY_OF_MONTH));

        dateTimeChip.setVisibility(GONE);
        isTimePicked = false;
        aggiornaLista();
    }

    private void showSnackBar(String text) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                text,
                Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        textView.setTextSize(14);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setTextColor(ContextCompat.getColor(this, R.color.secondaryColor));
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.tertiaryColor));

        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // NOTE:
        // LiveData should automatically remove destroyed observers but let's do it for clarity's sake
        alertsDAO.getUpdates().removeObservers(this);
        companiesDAO.getUpdates().removeObservers(this);
        transportsDAO.getUpdates().removeObservers(this);
        weatherDAO.getUpdates().removeObservers(this);
        weatherDAO.close();
    }


    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }


    private void aggiornaLista() {
        blurredBackground.setVisibility(VISIBLE);
        lottieLoader.setVisibility(VISIBLE);
        lottieLoader.playAnimation();
        if (!hasReceivedWeather || !hasReceivedTransports || !hasReceivedCompanies || !hasReceivedAlerts)
            return;

        analytics.send(ANALYTICS_CATEGORY_APP_EVENT, "Aggiorna Lista");

        selectMezzi = new ArrayList<>();

        aalvMezzi.clear();

        String expandedTransportName = espandiNave(nave);
        String expandedDepartureLocation = espandiPorto(portoPartenza);
        String expandedArrivalLocation = espandiPorto(portoArrivo);

        LocalDateTime selectedDate = LocalDateTime.ofInstant(c.toInstant(), c.getTimeZone().toZoneId());
        Log.d("selectedDate", "selectedDate " + selectedDate.toString());
        LocalDateTime oraLimite = selectedDate.plusDays(1);
        Log.d("selectedDate", "oraLimite " + oraLimite.toString());


        for (Mezzo mezzo : transportList) {
            LocalDateTime oraNave = selectedDate.toLocalDate().atTime(mezzo.getDepartureTime());

            if (oraNave.isBefore(selectedDate))
                oraNave = oraNave.plusDays(1);

            if (isNaveCompatibile(expandedTransportName, mezzo) &&
                    isPortoCompatibile(portoPartenza, expandedDepartureLocation, mezzo.portoPartenza) &&
                    isPortoCompatibile(portoArrivo, expandedArrivalLocation, mezzo.portoArrivo) &&
                    mezzo.isDateInExclusion(oraNave.toLocalDate()) &&
                    mezzo.isActiveOnDay(oraNave.getDayOfWeek()) &&
                    oraNave.isBefore(oraLimite)) {

                mezzo.setGiornoSeguente(!oraNave.toLocalDate().equals(selectedDate.toLocalDate()));

                selectMezzi.add(mezzo);
            }
        }

        selectMezzi.sort((m1, m2) -> {
            if (m1.getGiornoSeguente() == m2.getGiornoSeguente())
                return m1.getDepartureTime().compareTo(m2.getDepartureTime());
            else if (m1.getGiornoSeguente())
                return 1;
            else if (m2.getGiornoSeguente())
                return -1;

            return 0;
        });

        aalvMezzi.addAll(selectMezzi);

        aalvMezzi.notifyDataSetChanged();

        reportFullyDrawn();

        blurredBackground.setVisibility(GONE);
        lottieLoader.cancelAnimation();
        lottieLoader.setVisibility(GONE);
        GridView lvMezzi = findViewById(R.id.listMezzi);
        lvMezzi.smoothScrollToPosition(0);

    }

    private String espandiNave(String nave) {
        if (nave.contains(getString(R.string.traghetti)))
            return "Traghetto Caremar Medmar Ippocampo Ippocampo(da Chiaiolella) Ippocampo(a Chiaiolella) Traghetto LazioMar";
        if (nave.contains(getString(R.string.aliscafi)))
            return "Aliscafo Caremar Aliscafo SNAV Scotto Line Aliscafo Alilauro";
        if (nave.equals("Ippocampo"))
            return "Ippocampo Ippocampo(da Chiaiolella) Ippocampo(a Chiaiolella)";
        if (nave.contains("Gestur"))
            return "Motonave Gestur Traghetto Gestur";
        return nave;
    }

    private String espandiPorto(String porto) {
        switch (porto) {
            case "Napoli":
                return "Napoli Porta di Massa o Napoli Beverello";
            case "Napoli o Pozzuoli":
                return "Napoli Porta di Massa o Napoli Beverello o Pozzuoli";
            case "Ischia":
                return "Ischia Porto o Casamicciola";
            case "Monte di Procida":
                return "Monte di Procida";
            default:
                return porto;
        }
    }

    private boolean isNaveCompatibile(String naveEspanso, Mezzo mezzo) {
        return naveEspanso.contains(mezzo.nave) || nave.equals(getString(R.string.qualsiasi_imbarcazione));
    }

    private boolean isPortoCompatibile(String porto, String portoEspanso, String portoMezzo) {
        return portoMezzo.equals(porto) || portoEspanso.contains(portoMezzo) || porto.equals(getString(R.string.qualsiasi_porto));
    }

    private void setSpinner() {
        Spinner spnNave = findViewById(R.id.spnNave);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.strMezzi, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spnNave.setAdapter(adapter);

        nave = getString(R.string.qualsiasi_imbarcazione);
        portoPartenza = getString(R.string.qualsiasi_porto);
        portoArrivo = getString(R.string.qualsiasi_porto);

        spnNave.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                nave = parent.getItemAtPosition(pos).toString();
                aggiornaLista();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final Spinner spnPortoPartenza = findViewById(R.id.spnPortoPartenza);
        final ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this, R.array.strPorti, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(R.layout.spinner_item);
        spnPortoPartenza.setAdapter(adapter2);

        //controllo e setto tramite algoritmo di set con gps
        portoPartenza = setPortoPartenza();

        setSpnPortoPartenza(spnPortoPartenza, adapter2);

        final Spinner spnPortoArrivo = findViewById(R.id.spnPortoArrivo);
        final ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(
                this, R.array.strPorti, android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(R.layout.spinner_item);
        spnPortoArrivo.setAdapter(adapter3);

        if (!portoPartenza.contentEquals("Procida") || portoPartenza.contentEquals(getString(R.string.qualsiasi_porto))) {
            portoArrivo = "Procida";
            setSpnPortoArrivo(spnPortoArrivo, adapter3);
        }

        spnPortoPartenza.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                portoPartenza = parent.getItemAtPosition(pos).toString();
                if (portoPartenza.contentEquals("Procida") && portoArrivo.contentEquals("Procida")) {
                    portoArrivo = getString(R.string.qualsiasi_porto);
                    setSpnPortoArrivo(spnPortoArrivo, adapter2);
                    setSpnPortoPartenza(spnPortoPartenza, adapter3);
                } else if (!portoPartenza.contentEquals("Procida") || portoPartenza.contentEquals(getString(R.string.qualsiasi_porto))) {
                    portoArrivo = "Procida";
                    setSpnPortoArrivo(spnPortoArrivo, adapter2);
                    setSpnPortoPartenza(spnPortoPartenza, adapter3);
                }
                aggiornaLista();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        spnPortoArrivo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                portoArrivo = parent.getItemAtPosition(pos).toString();
                if (portoArrivo.contentEquals("Procida") && portoPartenza.contentEquals("Procida")) {
                    portoPartenza = getString(R.string.qualsiasi_porto);
                    setSpnPortoPartenza(spnPortoPartenza, adapter2);
                    setSpnPortoArrivo(spnPortoArrivo, adapter3);
                } else if (!portoArrivo.contentEquals("Procida") || portoArrivo.contentEquals(getString(R.string.qualsiasi_porto))) {
                    portoPartenza = "Procida";
                    setSpnPortoPartenza(spnPortoPartenza, adapter2);
                    setSpnPortoArrivo(spnPortoArrivo, adapter3);
                }
                aggiornaLista();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setSpnPortoArrivo(Spinner spnPortoArrivo, final ArrayAdapter<CharSequence> adapter3) {
        for (int i = 0; i < spnPortoArrivo.getCount(); i++) {
            if (adapter3.getItem(i).equals(portoArrivo)) {
                spnPortoArrivo.setSelection(i);
            }
        }
    }

    private void setSpnPortoPartenza(Spinner spnPortoPartenza, ArrayAdapter<CharSequence> adapter2) {
        for (int i = 0; i < spnPortoPartenza.getCount(); i++) {
            if (adapter2.getItem(i).equals(portoPartenza)) {
                spnPortoPartenza.setSelection(i);
            }
        }
    }

    private String setPortoPartenza() {
        // Trova il porto pi? vicino a quello di partenza
        Location l = null;


        if (ActivityCompat.checkSelfPermission(OrariProcida2011Activity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(OrariProcida2011Activity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //System.exit(0);
            return getString(R.string.qualsiasi_porto);
        }

        // l'accesso al GPS potrebbe non essere garantito per ragioni legate a Google Play
        // Bisogna gestire l'eccezione e restituire un valore di default che si potrà settare in altro punto dell'app
        try {
            l = myManager.getLastKnownLocation(BestProvider);
            Log.d("ACTIVITY", "Posizione:" + l.getLongitude() + "," + l.getLatitude());
        } catch (Exception e) {
            Log.e("Activity", "GPS: ", e);
        }
        if (l == null)
            return getString(R.string.qualsiasi_porto);
        //Coordinate angoli Procida
        if ((l.getLatitude() > 40.7374) && (l.getLatitude() < 40.7733) && (l.getLongitude() > 13.9897) && (l.getLongitude() < 14.0325)) {
            analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Procida");
            return "Procida";
        }
        //Coordinate angoli Isola d'Ischia
        if ((l.getLatitude() > 40.6921) && (l.getLatitude() < 40.7626) && (l.getLongitude() > 13.8465) && (l.getLongitude() < 13.9722))
            //Isola d'Ischia
            if (calcolaDistanza(l, 13.9063, 40.7496) > calcolaDistanza(l, 13.9602, 40.7319)) {
                analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Ischia");
                return "Ischia";
            } else {
                analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Casamicciola");
                return "Casamicciola";
            }
        //Inserire coordinate Napoli (media porti) e Pozzuoli
        double distNapoli = calcolaDistanza(l, 14.2575, 40.84);
        Log.d("OrariProcida", "d(Napoli)=" + distNapoli);
        double distPozzuoli = calcolaDistanza(l, 14.1179, 40.8239);
        Log.d("OrariProcida", "d(Pozzuoli)=" + distPozzuoli);
        double distMonteProcida = calcolaDistanza(l, 14.05, 40.8);
        if (distMonteProcida < 1500) {
            analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Monte di Procida");
            return "Monte di Procida";
        }
        if (distPozzuoli < distNapoli) {
            if (distPozzuoli < 15000) {
                analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Pozzuoli");
                return "Pozzuoli";
            } else {
                analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Napoli o Pozzuoli");
                return "Napoli o Pozzuoli";
            }
        } else {
            if (distNapoli < 15000) {
                if (distNapoli > 1000) {
                    analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Napoli");
                    return "Napoli";
                } else {
                    if (calcolaDistanza(l, 14.2548, 40.8376) < calcolaDistanza(l, 14.2602, 40.8424)) {
                        analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Napoli Beverello");
                        return "Napoli Beverello";
                    } else {
                        analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Napoli Porta di Massa");
                        return "Napoli Porta di Massa";
                    }
                }
            } else {
                analytics.send(ANALYTICS_CATEGORY_USER_EVENT, "From Napoli o Pozzuoli");
                return "Napoli o Pozzuoli";
            }
        }
    }

    private double calcolaDistanza(Location location, double lon, double lat) {
        //calcola distanza da obiettivo
        double deltaLong = Math.abs(lon - location.getLongitude());
        double deltaLat = Math.abs(lat - location.getLatitude());
        double delta = (Math.sqrt(deltaLong * deltaLong + deltaLat * deltaLat));
        delta = delta * 60 * 1852;
        return Math.ceil(delta);
    }

    private void onAlertsUpdate(AlertUpdate update) {
        hasReceivedAlerts = true;

        if (update.isValid()) {

            // FIXME: highly inefficient, transport list is sorted by time so it could be possible to do a binary search
            for (Alert alert : update.getData()) {
                for (Mezzo transport : transportList) {
                    if (sameTransport(transport, alert)) {
                        transport.addReason(alert.getReason());
                    }
                }
            }

            aggiornaLista();
        } else {
            // TODO: handle exception
            Log.e("MainActivity", "OnAlertsUpdate: ", update.getError());
            Toast.makeText(this, getString(R.string.error_update_alerts), Toast.LENGTH_SHORT).show();
        }
    }

    private void onCompaniesUpdate(CompaniesUpdate update) {
        hasReceivedCompanies = true;

        if (update.isValid()) {
            listCompagnia.clear();
            listCompagnia.addAll(update.getData());
        } else {
            // TODO: handle exception
            Log.e("MainActivity", "OnCompaniesUpdate: ", update.getError());
            Toast.makeText(this, getString(R.string.error_update_companies), Toast.LENGTH_SHORT).show();
        }
    }

    private void onTransportsUpdate(TransportsUpdate update) {
        boolean showToast = hasReceivedTransports;
        hasReceivedTransports = true;
        lottieLoader.setVisibility(VISIBLE);
        lottieLoader.playAnimation();
        if (update.isValid()) {
            transportList.clear();
            transportList.addAll(update.getData());
            aggiornaLista();

            alertsDAO.requestUpdate();

            if (showToast)
                showSnackBar(getString(R.string.orariAggiornatiAl) + " " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(update.getUpdateTime()));

        } else {
            // TODO: handle exception
            Log.e("MainActivity", "OnTransportsUpdate: ", update.getError());
            Toast.makeText(this, getString(R.string.error_update_transports), Toast.LENGTH_SHORT).show();
        }
        lottieLoader.setVisibility(GONE);
        lottieLoader.cancelAnimation();
        blurredBackground.setVisibility(GONE);
    }

    private void onWeatherUpdate(WeatherUpdate update) {
        boolean showToast = hasReceivedWeather;
        hasReceivedWeather = true;

        if (update.isValid()) {
            List<Osservazione> forecasts = update.getData();

            meteo.setForecasts(forecasts);

            aggiornaLista();

            // NOTE: before it would show a complete dialog, as of now I changed it to just display a toast
            if (showToast) {
                Osservazione forecast = meteo.getForecasts().get(0);
                String message = getString(R.string.updated) + " " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(forecast.getTime()) + "\n" +
                        getString(R.string.condimeteo) + " " + getWindBeaufortString(forecast) +
                        " (" + (int) forecast.getWindSpeed() + " km/h) " + getString(R.string.da) + " " + getWindDirectionString(forecast);

                showSnackBar(message);
            }

        } else {
            // TODO: handle exception
            Log.e("MainActivity", "OnWeatherUpdate: ", update.getError());
            Toast.makeText(this, getString(R.string.error_update_weather), Toast.LENGTH_SHORT).show();
        }
    }


    private String getWindBeaufortString(Osservazione forecast) {
        return getWindBeaufortString((int) forecast.getWindBeaufort());
    }

    private String getWindBeaufortString(int force) {
        switch (force) {
            case 0:
                return getString(R.string.calma);
            case 1:
                return getString(R.string.bavaDiVento);
            case 2:
                return getString(R.string.brezzaLeggera);
            case 3:
                return getString(R.string.brezzaTesa);
            case 4:
                return getString(R.string.ventoModerato);
            case 5:
                return getString(R.string.ventoTeso);
            case 6:
                return getString(R.string.ventoFresco);
            case 7:
                return getString(R.string.ventoForte);
            case 8:
                return getString(R.string.burrasca);
            case 9:
                return getString(R.string.burrascaForte);
            case 10:
                return getString(R.string.tempesta);
            case 11:
                return getString(R.string.fortunale);
            case 12:
                return getString(R.string.uragano);
        }

        return getString(R.string.errore);
    }

    private String getWindDirectionString(Osservazione forecast) {
        return getWindDirectionString(forecast.getWindDirection());
    }

    private String getWindDirectionString(Osservazione.Direction direction) {
        switch (direction) {
            case N:
                return getString(R.string.nord);
            case NW:
                return getString(R.string.nordOvest);
            case NE:
                return getString(R.string.nordEst);
            case E:
                return getString(R.string.est);
            case SE:
                return getString(R.string.sudEst);
            case S:
                return getString(R.string.sud);
            case SW:
                return getString(R.string.sudOvest);
            case W:
                return getString(R.string.ovest);
            default:
                return null; // NOTE: it can't happen, here just to make the compiler happy
        }
    }


    private boolean sameTransport(Mezzo transport, Alert alert) {
        LocalDate transportDate = LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

        if (transport.getGiornoSeguente())
            transportDate = transportDate.plusDays(1);

        String routeId = alert.getRouteId();
        String transportId = transport.getId();
        LocalDate alertDate = alert.getTransportDate();

        if (routeId == null || transportId == null || alertDate == null) {
            return false;
        }

        return routeId.equals(transportId) && alertDate.equals(transportDate);
    }


}
