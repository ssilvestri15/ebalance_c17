package com.c17.ebalance.ebalance.dati.service;

import com.c17.ebalance.ebalance.model.DAO.BatteriaDAO;
import com.c17.ebalance.ebalance.model.DAO.BatteriaDAOImpl;
import com.c17.ebalance.ebalance.model.DAO.ConsumoDAO;
import com.c17.ebalance.ebalance.model.DAO.ConsumoDAOImpl;
import com.c17.ebalance.ebalance.model.DAO.MeteoDAO;
import com.c17.ebalance.ebalance.model.DAO.MeteoDAOImpl;
import com.c17.ebalance.ebalance.model.DAO.ParametriIADAO;
import com.c17.ebalance.ebalance.model.DAO.ParametriIADAOImpl;
import com.c17.ebalance.ebalance.model.DAO.ProduzioneDAO;
import com.c17.ebalance.ebalance.model.DAO.ProduzioneDAOImpl;
import com.c17.ebalance.ebalance.model.entity.InteragisceBean;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Implementazione dell'interfaccia {@link SimulazioneService} che fornisce metodi per simulare dati nel sistema eBalance.
 * Questa classe si occupa di simulare il consumo, la produzione e altri parametri in modo da mantenere il sistema attivo.
 * La simulazione viene eseguita a intervalli orari durante il giorno.
 */
public class SimulazioneServiceImpl implements SimulazioneService {
    private ConsumoDAO consumoDAO = new ConsumoDAOImpl();
    private ProduzioneDAO produzioneDAO = new ProduzioneDAOImpl();
    private BatteriaDAO batteriaDAO = new BatteriaDAOImpl();
    private MeteoDAO meteoDAO = new MeteoDAOImpl();
    private ParametriIADAO parametriIADAO = new ParametriIADAOImpl();
    private Random random = new Random();
    private Calendar calendario = Calendar.getInstance();
    private Date data;
    private int y = 0;
    float percentualeEccesso = 0.00f;
    private boolean simulazioneVenditaFlag = true; //setta a true se vuoi far simulare la generazione di una vendita

    /**
     * Esegue la simulazione dei dati nel sistema eBalance.
     *
     * @throws SQLException Se si verifica un errore durante l'esecuzione della simulazione o l'accesso al database.
     */
    @Override
    public void simulazione() throws SQLException {
        int numBatterie = batteriaDAO.ottieniNumBatterieAttive();
        List<String> condizioni = meteoDAO.getCondizione();
        data = calendario.getTime();
        java.sql.Date sqlDate = new java.sql.Date(data.getTime());
        float consumoOrarioAttualeTot = 0.00f;
        float produzioneOrariaAttualeTot = 0.00f;
        try {
            for (int i = 0; i < 24; i++) {
                List<InteragisceBean> parametriAttivi = parametriIADAO.ottieniParametriAttivi();

                consumoOrarioAttualeTot = simulaConsumi(numBatterie, sqlDate, parametriAttivi);

                produzioneOrariaAttualeTot = simulaProduzioneSorgenti(numBatterie, sqlDate, parametriAttivi);

                if (consumoOrarioAttualeTot > produzioneOrariaAttualeTot) {
                    float produzioneNecessaria = (float) (Math.round((consumoOrarioAttualeTot - produzioneOrariaAttualeTot) * 100.0) / 100.0);
                    simulaSEN(produzioneNecessaria, numBatterie, sqlDate, parametriAttivi);
                }

                if (i == 0 || i == 6 || i == 12 || i == 18) {
                    if (!meteoDAO.verificaPresenza(sqlDate, i)) {
                        simulaPrevisioniMeteo(condizioni, sqlDate, i);
                    }
                }
                Thread.sleep(10000); // Ritardo di 10 secondi
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        calendario.add(Calendar.DAY_OF_YEAR, 1);

    }

    /**
     * Simula il consumo degli edifici e aggiorna i dati nel database.
     *
     * @param numBatterie Numero di batterie attive nel sistema.
     * @param sqlDate     Data per la quale simulare il consumo.
     * @param parametriAttivi Lista dei parametri attivi per la simulazione.
     * @return Il consumo totale orario simulato.
     * @throws SQLException Se si verifica un errore durante l'accesso al database.
     */
    private float simulaConsumi(int numBatterie, java.sql.Date sqlDate, List<InteragisceBean> parametriAttivi) throws SQLException {
        Random random = new Random();
        int numEdifici = consumoDAO.ottieniNumEdifici();
        float consumoOrarioAttualeTot = 0.00f;
        for (int y = 0; y < numEdifici; y++) {
            float consumoOrario = 0.00f;
            if (simulazioneVenditaFlag) {
                consumoOrario = random.nextFloat() * 9 + 3;
            } else {
                consumoOrario = random.nextFloat() * 15 + 15;
            }
            consumoOrario = (float) (Math.round(consumoOrario * 100.0) / 100.0);
            consumoDAO.simulaConsumo(consumoOrario, y + 1, sqlDate);
            percentualeEccesso = batteriaDAO.aggiornaBatteria((-consumoOrario) / numBatterie, numBatterie);
            if (percentualeEccesso < 0) {
                parametriIADAO.aggiornaPercentualeSEN(10);
            }
            consumoOrarioAttualeTot = consumoOrarioAttualeTot + consumoOrario;
        }
        return consumoOrarioAttualeTot;
    }

    /**
     * Simula la produzione delle sorgenti e aggiorna i dati nel database.
     *
     * @param numBatterie Numero di batterie attive nel sistema.
     * @param sqlDate     Data per la quale simulare la produzione.
     * @param parametriAttivi Lista dei parametri attivi per la simulazione.
     * @return La produzione totale oraria simulata.
     * @throws SQLException Se si verifica un errore durante l'accesso al database.
     */
    private float simulaProduzioneSorgenti(int numBatterie, java.sql.Date sqlDate, List<InteragisceBean> parametriAttivi) throws SQLException {
        float produzioneOrariaAttualeTot = 0.02f;
        Random random2 = new Random();
        int sorgentiAttive = produzioneDAO.ottieniSorgenti();
        for (int y = 1; y < sorgentiAttive; y++) {
            float produzioneOraria = 0.00f;
            if (simulazioneVenditaFlag) {
                produzioneOraria = random2.nextFloat() * 200 + 100;
            } else {
                produzioneOraria = random2.nextFloat() * 100 + 0;
            }
            produzioneOraria = (float) (Math.round(produzioneOraria * 100.0) / 100.0);
            for (InteragisceBean bean : parametriAttivi) {
                if (bean.getTipoSorgente().equalsIgnoreCase("Pannello Fotovoltaico")) {
                    produzioneOraria = produzioneOraria * ((float) bean.getPercentualeUtilizzoSorgente() / 100);
                    break;
                }
            }
            produzioneDAO.simulaProduzione(y + 1, produzioneOraria, sqlDate);
            percentualeEccesso = batteriaDAO.aggiornaBatteria((produzioneOraria) / numBatterie, numBatterie);
            if (percentualeEccesso > 0) {
                parametriIADAO.aggiornaPercentualeSEN(-10);
            }
            produzioneOrariaAttualeTot = produzioneOrariaAttualeTot + produzioneOraria;
        }
        return produzioneOrariaAttualeTot;
    }

    /**
     * Simula la produzione del Servizio Elettrico Nazionale (SEN) per coprire il consumo e aggiorna i dati nel database.
     *
     * @param produzioneNecessaria Produzione necessaria dal SEN.
     * @param numBatterie Numero di batterie attive nel sistema.
     * @param sqlDate     Data per la quale simulare la produzione del SEN.
     * @param parametriAttivi Lista dei parametri attivi per la simulazione.
     * @throws SQLException Se si verifica un errore durante l'accesso al database.
     */
    private void simulaSEN(float produzioneNecessaria, int numBatterie, java.sql.Date sqlDate, List<InteragisceBean> parametriAttivi) throws SQLException {
        for (InteragisceBean bean : parametriAttivi) {
            if (bean.getTipoSorgente().equalsIgnoreCase("Servizio Elettrico Nazionale")) {
                produzioneNecessaria = produzioneNecessaria * ((float) bean.getPercentualeUtilizzoSorgente() / 100);
                break;
            }
        }
        produzioneDAO.simulaProduzioneSEN(produzioneNecessaria, sqlDate);
        percentualeEccesso = batteriaDAO.aggiornaBatteria((produzioneNecessaria) / numBatterie, numBatterie);
        if (percentualeEccesso > 0) {
            parametriIADAO.aggiornaPercentualeSEN(-10);
        }
    }

    /**
     * Simula le previsioni meteo per una specifica data e ora e aggiorna i dati nel database.
     *
     * @param condizioni Lista delle condizioni meteo possibili.
     * @param sqlDate     Data per la quale simulare le previsioni meteo.
     * @param i           Ora del giorno per la quale simulare le previsioni meteo.
     * @throws SQLException Se si verifica un errore durante l'accesso al database.
     */
    private void simulaPrevisioniMeteo(List<String> condizioni, java.sql.Date sqlDate, int i) throws SQLException {
        float vel = random.nextFloat() * 10;
        vel = (float) (Math.round(vel * 100.0) / 100.0);
        int indiceCasuale = random.nextInt(condizioni.size());
        String condizioneCasuale = condizioni.get(indiceCasuale);
        int prob = 0;
        if (condizioneCasuale.equalsIgnoreCase("nevoso")) {
            prob = random.nextInt(10) + 30;
        }
        if (condizioneCasuale.equalsIgnoreCase("nuvoloso")) {
            prob = random.nextInt(10) + 30;
        }
        if (condizioneCasuale.equalsIgnoreCase("piovoso")) {
            prob = random.nextInt(20) + 40;
        }
        if (condizioneCasuale.equalsIgnoreCase("sereno")) {
            prob = random.nextInt(20);
        }
        if (condizioneCasuale.equalsIgnoreCase("soleggiato")) {
            prob = random.nextInt(10);
        }
        if (condizioneCasuale.equalsIgnoreCase("ventilato")) {
            prob = random.nextInt(10) + 20;
        }
        meteoDAO.insertPrevisioni(sqlDate, i, vel, prob, condizioneCasuale);

        int sorgentiAttive = produzioneDAO.ottieniSorgenti();
        int id = meteoDAO.getParametro(sqlDate, i);
        for(int indice = 1; indice < sorgentiAttive + 1 ; indice++) {
            meteoDAO.aggiornaInfluenzare(id, indice);
        }
    }

    /**
     * Inserisce le previsioni iniziali nel sistema eBalance.
     *
     * @throws SQLException Se si verifica un errore durante l'inserimento delle previsioni iniziali o l'accesso al database.
     */
    @Override
    public void insertPrevisioniIniziali() throws SQLException {

        List<String> condizioni = meteoDAO.getCondizione();
        for (int y = 0; y < 6; y++) {
            data = calendario.getTime();
            java.sql.Date sqlDate = new java.sql.Date(data.getTime());
            for (int i = 0; i < 24; i = i + 6) {
                if (!meteoDAO.verificaPresenza(sqlDate, i)) {
                    simulaPrevisioniMeteo(condizioni, sqlDate, i);
                }
            }
            calendario.add(Calendar.DAY_OF_YEAR, 1);
        }
    }
}