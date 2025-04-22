package com.porfirio.orariprocida2011.adapter;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;

import com.porfirio.orariprocida2011.R;
import com.porfirio.orariprocida2011.entity.CompanyEnum;
import com.porfirio.orariprocida2011.entity.Meteo;
import com.porfirio.orariprocida2011.entity.Mezzo;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


public class MezzoAdapter extends ArrayAdapter<Mezzo> {

    private Context context;
    private List<Mezzo> mezziList;
    private ImageView iconLogo;
    private TextView textViewWarning;
    private Meteo meteo;
    Calendar calen;

    public MezzoAdapter(Context context, List<Mezzo> mezziList, Calendar calen) {
        super(context, R.layout.list_item, mezziList);
        this.context = context;
        this.mezziList = mezziList;
        this.calen = calen;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        }

        Mezzo mezzo = mezziList.get(position);
        meteo = new Meteo();

        TextView textViewMezzo = convertView.findViewById(R.id.text_view_list_item_mezzo);
        textViewMezzo.setText(mezzo.nave);

        TextView textViewPartenzaArrivo = convertView.findViewById(R.id.text_view_list_item_Partenza_Arrivo);
        textViewPartenzaArrivo.setText(String.format("%s - %s", mezzo.portoPartenza, mezzo.portoArrivo));

        TextView textViewOrario = convertView.findViewById(R.id.text_view_list_item_Orario);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(calen.getTime());


        LocalTime currentTime = LocalTime.of(calen.get(Calendar.HOUR_OF_DAY), calen.get(Calendar.MINUTE));
        LocalTime departureDateTime =  mezzo.getDepartureTime();

        //se l'orario scelto è prima dell'attuale e il giorno non è stato scelto (quindi sarebbe quello corrente del dispositivo) imposta come giorno quello successivo
        if (departureDateTime.isBefore(currentTime)) {
            Calendar newDate = (Calendar) calen.clone();
            newDate.add(Calendar.DAY_OF_MONTH, 1);
            formattedDate = dateFormat.format(newDate.getTime());
        }

        textViewOrario.setText(mezzo.getDepartureTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) + " - " + formattedDate);

        iconLogo = convertView.findViewById(R.id.image_view_icon_logo);
        iconLogo.setImageResource(getIconForCompany(mezzo.nave));


        textViewWarning = convertView.findViewById(R.id.text_view_warning);
        Animation blinkAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.blink);
        if (mezzo.tot > 0 || !getWeatherConditionsString(getContext(), mezzo, calen).isEmpty()) {
            textViewWarning.setVisibility(VISIBLE);
            textViewWarning.startAnimation(blinkAnimation);
        } else {
            textViewWarning.setVisibility(INVISIBLE);
            textViewWarning.clearAnimation();
        }


        return convertView;
    }
    private String getWeatherConditionsString(Context context, Mezzo route, Calendar calen) {
        double extraWind = meteo.getForecast(context, route, calen);
        if (extraWind <= 0)
            return "";
        else if (extraWind <= 1)
            return " - " + context.getString(R.string.pocoProbabile);
        else if (extraWind <= 2)
            return " - " + context.getString(R.string.aRischio);
        else if (extraWind <= 3)
            return " - " + context.getString(R.string.corsaQuasi);
        else
            return " - " + context.getString(R.string.corsaImpossibile);
    }

    private int getIconForCompany(String mezzoNome) {
        return CompanyEnum.findDrawByName(mezzoNome.toLowerCase().trim());
    }
}
