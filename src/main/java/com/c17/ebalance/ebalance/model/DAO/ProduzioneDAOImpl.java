package com.c17.ebalance.ebalance.model.DAO;

import com.c17.ebalance.ebalance.model.entity.ArchivioProduzioneBean;
import com.c17.ebalance.ebalance.model.entity.ProduzioneAttualeBean;
import com.c17.ebalance.ebalance.model.entity.SorgenteBean;
import com.c17.ebalance.ebalance.model.entity.TipoSorgenteBean;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione dell'interfaccia ProduzioneDAO per l'accesso ai dati relativi alla produzione di energia.
 */
public class ProduzioneDAOImpl implements ProduzioneDAO {

    private static Logger logger =
            Logger.getLogger(ProduzioneDAOImpl.class.getName());
    private static DataSource ds;

    static {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");

            ds = (DataSource) envCtx.lookup("jdbc/ebalance");

        } catch (NamingException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    private static final String TABLE_NAME_ARCHIVIO = "ArchivioProduzione";
    private static final String TABLE_NAME_SORGENTE = "Sorgente";
    private static final String TABLE_NAME_TIPO_SORGENTE = "TipoSorgente";
    private static final String TABLE_NAME_CONSUMO = "ConsumoEdificio";


    /**
     * Restituisce una lista di oggetti ArchivioProduzioneBean rappresentanti la produzione storica.
     *
     * @return Lista di ArchivioProduzioneBean
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    public List<ArchivioProduzioneBean> visualizzaProduzione() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<ArchivioProduzioneBean> produzione = new ArrayList<>();
        String selectSQL = "SELECT * FROM " + TABLE_NAME_ARCHIVIO;


        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                ArchivioProduzioneBean bean = new ArchivioProduzioneBean();
                bean.setIdProduzione(resultSet.getInt("IdProduzione"));
                bean.setDataProduzione(resultSet.getDate("DataProduzione"));
                bean.setProduzioneGiornaliera(resultSet.getFloat("ProduzioneGiornaliera"));
                produzione.add(bean);
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return produzione;
    }


    /**
     * Restituisce una lista di oggetti SorgenteBean rappresentanti le informazioni sulla produzione per ciascuna sorgente.
     *
     * @return Lista di SorgenteBean
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public List<SorgenteBean> visualizzaProduzioneSorgente() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<SorgenteBean> produzione = new ArrayList<>();
        String selectSQL = "SELECT * FROM " + TABLE_NAME_SORGENTE;

        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                SorgenteBean bean = new SorgenteBean();
                bean.setIdSorgente(resultSet.getInt("IdSorgente"));
                bean.setProduzioneAttuale(resultSet.getFloat("ProduzioneAttuale"));
                bean.setDataInstallazione(resultSet.getDate("DataInstallazione"));
                bean.setTipologia(resultSet.getString("Tipologia"));
                bean.setFlagAttivazioneSorgente(resultSet.getBoolean("FlagAttivazioneSorgente"));
                bean.setFlagStatoSorgente(resultSet.getBoolean("FlagStatoSorgente"));
                produzione.add(bean);
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return produzione;
    }

    /**
     * Restituisce la produzione totale delle sorgenti rinnovabili, escludendo il Servizio Elettrico Nazionale.
     *
     * @return Produzione totale delle sorgenti rinnovabili
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public float ottieniProduzioneProdotta() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ProduzioneAttualeBean produzioneSorgente = new ProduzioneAttualeBean();
        String selectSQL = "SELECT  ROUND(SUM(ProduzioneAttuale),2) AS ProduzioneSorgente FROM " + TABLE_NAME_SORGENTE
                + " WHERE Tipologia != 'Servizio Elettrico Nazionale'";
        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                produzioneSorgente.setProduzioneAttuale(resultSet.getFloat("ProduzioneSorgente"));
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return produzioneSorgente.getProduzioneAttuale();

    }

    /**
     * Restituisce la produzione del Servizio Elettrico Nazionale (SEN).
     *
     * @return Produzione del SEN
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    public float ottieniProduzioneSEN() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        ProduzioneAttualeBean produzioneSEN = new ProduzioneAttualeBean();
        String selectSQL = "SELECT  ROUND(ProduzioneAttuale,2) AS ProduzioneSEN FROM " + TABLE_NAME_SORGENTE
                + " WHERE Tipologia = 'Servizio Elettrico Nazionale'";
        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                produzioneSEN.setProduzioneAttuale(resultSet.getFloat("ProduzioneSEN"));
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return produzioneSEN.getProduzioneAttuale();

    }

    /**
     * Restituisce una lista di oggetti TipoSorgenteBean rappresentanti i tipi di sorgenti disponibili.
     *
     * @return Lista di TipoSorgenteBean
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public List<TipoSorgenteBean> ottieniTipoSorgente() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        List<TipoSorgenteBean> tipoSorgente = new ArrayList<>();
        String selectSQL = "SELECT * FROM " + TABLE_NAME_TIPO_SORGENTE;

        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                TipoSorgenteBean bean = new TipoSorgenteBean();
                bean.setTipo(resultSet.getString("Tipo"));
                tipoSorgente.add(bean);
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return tipoSorgente;
    }

    /**
     * Simula la produzione di una sorgente in una data specifica.
     *
     * @param idSorgente         ID della sorgente da simulare
     * @param produzioneSimulata Quantità di produzione simulata
     * @param data               Data della simulazione
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public void simulaProduzione(int idSorgente, float produzioneSimulata, Date data) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement preparedStatement2 = null;
        PreparedStatement preparedStatement3 = null;
        PreparedStatement preparedStatement4 = null;
        PreparedStatement preparedStatement5 = null;

        String selectSQL = "SELECT FlagAttivazioneSorgente, FlagStatoSorgente FROM " + TABLE_NAME_SORGENTE
                + " WHERE IdSorgente = ?";
        String updateSQL = "UPDATE " + TABLE_NAME_SORGENTE + " SET ProduzioneAttuale = ? WHERE IdSorgente = ?";
        String selectSQL2 = "SELECT * FROM " + TABLE_NAME_ARCHIVIO + " WHERE IdSorgente = ? AND DataProduzione = ?";
        String insertSQL = "INSERT INTO " + TABLE_NAME_ARCHIVIO + " (DataProduzione, ProduzioneGiornaliera, IdSorgente) VALUES (?, 0, ?)";
        String updateSQL2 = "UPDATE " + TABLE_NAME_ARCHIVIO + " SET ProduzioneGiornaliera = ProduzioneGiornaliera + ? WHERE IdSorgente = ? AND DataProduzione = ?";

        try {
            connection = ds.getConnection();

            preparedStatement = connection.prepareStatement(selectSQL);
            preparedStatement.setInt(1, idSorgente);
            ResultSet resultset = preparedStatement.executeQuery();

            while (resultset.next()) {
                if (resultset.getBoolean("FlagStatoSorgente") && resultset.getBoolean("FlagAttivazioneSorgente")) {
                    preparedStatement2 = connection.prepareStatement(updateSQL);
                    preparedStatement2.setFloat(1, produzioneSimulata);
                    preparedStatement2.setInt(2, idSorgente);
                    preparedStatement2.executeUpdate();

                    preparedStatement3 = connection.prepareStatement(selectSQL2);
                    preparedStatement3.setInt(1, idSorgente);
                    preparedStatement3.setDate(2, data);
                    ResultSet resultSet2 = preparedStatement3.executeQuery();

                    if (!resultSet2.next()) {
                        preparedStatement4 = connection.prepareStatement(insertSQL);
                        preparedStatement4.setDate(1, data);
                        preparedStatement4.setInt(2, idSorgente);
                        preparedStatement4.executeUpdate();
                    }

                    preparedStatement5 = connection.prepareStatement(updateSQL2);
                    preparedStatement5.setFloat(1, produzioneSimulata);
                    preparedStatement5.setInt(2, idSorgente);
                    preparedStatement5.setDate(3, data);
                    preparedStatement5.executeUpdate();
                }
            }
        } finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (preparedStatement2 != null) preparedStatement2.close();
                if (preparedStatement3 != null) preparedStatement3.close();
                if (preparedStatement4 != null) preparedStatement4.close();
                if (preparedStatement5 != null) preparedStatement5.close();
            } finally {
                if (connection != null) connection.close();
            }
        }
    }

    /**
     * Restituisce il numero totale di sorgenti.
     *
     * @return Numero totale di sorgenti
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public int ottieniSorgenti() throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        int numSorgente = 0;
        String selectSQL = "SELECT COUNT(IdSorgente) AS IdSorgente FROM " + TABLE_NAME_SORGENTE;
        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                numSorgente = resultSet.getInt("IdSorgente");
            }
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }
        }
        return numSorgente;
    }


    /**
     * Simula la produzione del Servizio Elettrico Nazionale (SEN) in una data specifica.
     *
     * @param produzioneNecessaria Quantità di produzione necessaria per il SEN
     * @param data                Data della simulazione
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public void simulaProduzioneSEN(float produzioneNecessaria, Date data) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        PreparedStatement preparedStatement2 = null;
        PreparedStatement preparedStatement3 = null;
        PreparedStatement preparedStatement4 = null;
        PreparedStatement preparedStatement5 = null;

        String selectSQL = "SELECT FlagAttivazioneSorgente, FlagStatoSorgente FROM " + TABLE_NAME_SORGENTE
                + " WHERE IdSorgente = 1";
        String updateSQL = "UPDATE " + TABLE_NAME_SORGENTE + " SET ProduzioneAttuale = ? WHERE IdSorgente = 1";
        String selectSQL2 = "SELECT * FROM " + TABLE_NAME_ARCHIVIO + " WHERE IdSorgente = 1 AND DataProduzione = ?";
        String insertSQL = "INSERT INTO " + TABLE_NAME_ARCHIVIO + " (DataProduzione, ProduzioneGiornaliera, IdSorgente) VALUES (?, 0, 1)";
        String updateSQL2 = "UPDATE " + TABLE_NAME_ARCHIVIO + " SET ProduzioneGiornaliera = ProduzioneGiornaliera + ? WHERE IdSorgente = 1 AND DataProduzione = ?";

        try {
            connection = ds.getConnection();

            preparedStatement = connection.prepareStatement(selectSQL);
            ResultSet resultset = preparedStatement.executeQuery();

            while (resultset.next()) {
                if (resultset.getBoolean("FlagStatoSorgente") && resultset.getBoolean("FlagAttivazioneSorgente")) {
                    preparedStatement2 = connection.prepareStatement(updateSQL);
                    preparedStatement2.setFloat(1, produzioneNecessaria);
                    preparedStatement2.executeUpdate();

                    preparedStatement3 = connection.prepareStatement(selectSQL2);
                    preparedStatement3.setDate(1, data);
                    ResultSet resultSet2 = preparedStatement3.executeQuery();

                    if (!resultSet2.next()) {
                        preparedStatement4 = connection.prepareStatement(insertSQL);
                        preparedStatement4.setDate(1, data);
                        preparedStatement4.executeUpdate();
                    }

                    preparedStatement5 = connection.prepareStatement(updateSQL2);
                    preparedStatement5.setFloat(1, produzioneNecessaria);
                    preparedStatement5.setDate(2, data);
                    preparedStatement5.executeUpdate();
                }
            }
        } finally {
            try {
                if (preparedStatement != null) preparedStatement.close();
                if (preparedStatement2 != null) preparedStatement2.close();
                if (preparedStatement3 != null) preparedStatement3.close();
                if (preparedStatement4 != null) preparedStatement4.close();
                if (preparedStatement5 != null) preparedStatement5.close();
            } finally {
                if (connection != null) connection.close();
            }
        }

    }


    /**
     * Restituisce la quantità totale di energia rinnovabile prodotta in un intervallo di date.
     *
     * @param dataInizio Data di inizio dell'intervallo
     * @param dataFine   Data di fine dell'intervallo
     * @return Quantità totale di energia rinnovabile prodotta
     * @throws SQLException Se si verifica un errore durante l'accesso ai dati.
     */
    @Override
    public float energiaRinnovabileProdottaPerData(final Date dataInizio, final Date dataFine) throws SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        float energiaTotale = 0;
        String selectSQL = "SELECT SUM(ProduzioneGiornaliera) AS Produzione FROM "
                + TABLE_NAME_ARCHIVIO + " WHERE IdSorgente != 1 AND DataProduzione BETWEEN ? AND ? ";
        try {
            connection = ds.getConnection();
            preparedStatement = connection.prepareStatement(selectSQL);
            preparedStatement.setDate(1, dataInizio);
            preparedStatement.setDate(2, dataFine);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                energiaTotale = resultSet.getFloat("Produzione");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
        }
        return energiaTotale;
    }
}
