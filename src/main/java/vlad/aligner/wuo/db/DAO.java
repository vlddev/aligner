/*
#######################################################################
#
#  vlad-aligner - parallel texts aligner
#
#  Copyright (C) 2009-2010 Volodymyr Vlad
#
#  This file is part of vlad-aligner.
#
#  Foobar is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Foobar is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
#
#######################################################################
*/

package vlad.aligner.wuo.db;

import java.sql.*;
import java.util.logging.Logger;

public class DAO {
	
	private static final Logger	logger = Logger.getLogger(DAO.class.getName());

    /** Make connection to database.
     */
    public static Connection getConnection(String user, String password, String dbUrl, String jdbcDriver){
        Connection con = null;
        try{
            java.sql.Driver drv = (java.sql.Driver)Class.forName(jdbcDriver).newInstance();
            DriverManager.registerDriver(drv);
            try{
                if(user != null)
                    con = DriverManager.getConnection(dbUrl,user,password);
                else
                    con = DriverManager.getConnection(dbUrl);
                System.out.println("Connected to DB "+dbUrl);
                con.setAutoCommit(false);
            }catch(SQLException e){
                e.printStackTrace();
            }
        }catch(Exception e){
            System.out.println("Can't load driver "+jdbcDriver);
            System.out.println("Exception "+e.getClass().getName()+
            " in makeConnection : "+e.getMessage());
        }
        return con;
    }
	
    /**
     * Closes the SQL Statement and handles null check and exception handling.
     * @param statement The Statement to close.
     */
    public static void closeStatement(final Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Exception e) {
            logger.warning("DaoDB: Exception closing Statement: " + e.getMessage());
        }
    }

	/**
     * Closes the ResultSet and handles null check and exception handling.
     * @param resultSet The ResultSet to close
     */
    public static void closeResultSet(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Exception e) {
            logger.warning("DaoDB: Exception closing ResultSet: " + e.getMessage());
        }
    }

    public static void closeConnection(final Connection con) {
        try {
            if (con != null) {
                if (!con.getAutoCommit())
                    rollback(con);
                con.close();
            }
        } catch (Exception e) {
            logger.warning("DaoDB: Exception closing Connection: " + e.getMessage());
        }
    }

    public static void rollback(final Connection con) {
        if (con == null) {
            return;
        }
        try {
            con.rollback();
        } catch (Exception e) {
        	logger.warning("DaoDB: Rollback failed: " + e.getMessage());
        }
    }
}
