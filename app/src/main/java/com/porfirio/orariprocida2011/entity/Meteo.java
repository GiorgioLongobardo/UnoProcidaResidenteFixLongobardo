package com.porfirio.orariprocida2011.entity;

import android.content.Context;

import com.porfirio.orariprocida2011.R;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Meteo {

    private static List<Osservazione> forecasts = new ArrayList<>();

    public double getForecast(Context context, Mezzo route, Calendar calen) {

        if (forecasts.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDate selectedDate = LocalDate.of(calen.get(Calendar.YEAR), calen.get(Calendar.MONTH) + 1, calen.get(Calendar.DAY_OF_MONTH));

        LocalDateTime departureTime = selectedDate.atTime(route.getDepartureTime());
        // NOTE: workaround because departure time doesn't save the actual day so it may be checking the next-day route
        if (departureTime.isBefore(now)) {
            departureTime = departureTime.plusDays(1);
        }

        int hoursUntilDeparture = (int) Duration.between(now, departureTime).getSeconds() / (60 * 60);
        int forecastIndex = hoursUntilDeparture / 3;
        double limitBeaufort = 0.0;
        double actualBeaufort = 0.0;

        if (forecastIndex < forecasts.size()) {
            actualBeaufort = forecasts.get(forecastIndex).getWindBeaufort();
        }else{
            //caso in cui la corsa è troppo avanti nel tempo e non si hanno le previsioni per quell'ora
            return 0;
        }

        if (isSummer(now)) {
            limitBeaufort += 2;
        }

        // Penalità/Bonus in base al mezzo
        if (route.nave.equals("Procida Lines") || route.nave.equals("Gestur") || route.nave.contains("Ippocampo") || route.nave.contains("Aladino")) {
            limitBeaufort -= 1;
        } else if (route.nave.equals(context.getString(R.string.aliscafo) + " SNAV")) {
            limitBeaufort -= 0.5;
        }

        if ((departureTime.getHour() == 7 && departureTime.getMinute() == 40) ||
                (departureTime.getHour() == 19 && departureTime.getMinute() == 25) ||
                (departureTime.getHour() == 6 && departureTime.getMinute() == 25) ||
                (departureTime.getHour() == 20 && departureTime.getMinute() == 0)) {
            limitBeaufort += 1;
        }

        if (forecastIndex < forecasts.size()) {
            Osservazione.Direction windDirection = forecasts.get(forecastIndex).getWindDirection();

            if (windDirection == Osservazione.Direction.N || windDirection == Osservazione.Direction.NW) {
                if (route.portoArrivo.contains("Ischia") || route.portoPartenza.contains("Ischia") ||
                        route.portoArrivo.contains("Casamicciola") || route.portoPartenza.contains("Casamicciola")) {
                    limitBeaufort += 4;
                } else if (route.portoArrivo.contains("Napoli") || route.portoPartenza.contains("Napoli") ||
                        route.portoArrivo.contentEquals("Pozzuoli") || route.portoPartenza.contentEquals("Pozzuoli")) {
                    limitBeaufort += 5;
                }
            } else if (windDirection == Osservazione.Direction.NE || windDirection == Osservazione.Direction.E) {
                limitBeaufort += 4;
            } else if (windDirection == Osservazione.Direction.SE || windDirection == Osservazione.Direction.S || windDirection == Osservazione.Direction.SW) {
                if (route.nave.contains("Aliscafo")) {
                    limitBeaufort += 3;
                } else {
                    limitBeaufort += 4;
                }
            } else if (windDirection == Osservazione.Direction.W) {
                limitBeaufort += 3;
            }
        }

        if (route.portoPartenza.contentEquals("Monte di Procida") || route.portoArrivo.contentEquals("Monte di Procida")) {
            limitBeaufort += 4;
        }


        return actualBeaufort - limitBeaufort;
    }

    public List<Osservazione> getForecasts() {
        return forecasts;
    }

    public void setForecasts(List<Osservazione> forecasts) {
        this.forecasts = forecasts;
    }

    private boolean isSummer(LocalDateTime time) {
        int month = time.getMonthValue();
        return month >= 5 && month <= 8;
    }

}
