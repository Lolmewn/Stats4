package nl.lolmewn.stats.database;

import nl.lolmewn.stats.Statistic;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class GenericStorage {

    private static Set<String> cachedCreatedTables = new HashSet<>();

    public static void init() throws SQLException {
        try(Connection con = MySQLThreadPool.getInstance().getConnection()){
            Statement st = con.createStatement();
            ResultSet set = st.executeQuery("SHOW TABLES");
            while(set != null && set.next()){
                cachedCreatedTables.add(set.getString(1));
            }
        }
    }

    public static void save(Statistic statistic, Object... variables) throws SQLException {
        // Sure thing.
        if(!cachedCreatedTables.contains(statistic.table())){
            if(!createTable(statistic, variables)){
                throw new IllegalStateException("Table creation failed, cannot store record for " + statistic.table());
            }
        }
    }

    private static boolean createTable(Statistic statistic, Object[] variables) throws SQLException {
        if(statistic.variables().length != variables.length){
            System.err.println("Could not create table " + statistic.table() + ", variable count does not equal specified table variable count");
            return false;
        }
        System.out.println("Attempting to create new table " + statistic.table() + "...");

        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(statistic.table());
        sb.append("(");
        for(int i = 0; i < statistic.variables().length; i++){
            if(i != 0){
                sb.append(", ");
            }
            sb.append(statistic.variables()[i]).append(" ").append(getType(variables[i]));
        }
        sb.append(")");

        try(Connection con = MySQLThreadPool.getInstance().getConnection()){
            con.createStatement().execute(sb.toString());
        }

        cachedCreatedTables.add(statistic.table());
        return true;
    }

    private static String getType(Object variable) {
        if(variable instanceof String){
            return "TEXT";
        }
        if(variable instanceof Long){
            return "BIGINT";
        }
        if(variable instanceof Boolean){
            return "BOOLEAN";
        }
        if(variable instanceof Integer){
            return "INT";
        }
        if(variable instanceof Float){
            return "FLOAT";
        }
        if(variable instanceof Double){
            return "DOUBLE";
        }
        System.out.println("Unknown data type, investigate: " + variable);
        return "TEXT";
    }
}
