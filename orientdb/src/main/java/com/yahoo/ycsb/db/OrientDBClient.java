/**
 * OrientDB client binding for YCSB.
 *
 * Submitted by Luca Garulli on 5/10/2012.
 *
 */

package com.yahoo.ycsb.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.ldbc.DB;
import com.ldbc.DBException;
import com.ldbc.util.ByteIterator;
import com.ldbc.util.StringByteIterator;
import com.ldbc.util.Utils;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * OrientDB client for YCSB framework.
 * 
 * Properties to set:
 * 
 * orientdb.url=local:C:/temp/databases or remote:localhost:2424 <br>
 * orientdb.database=ycsb <br>
 * orientdb.user=admin <br>
 * orientdb.password=admin <br>
 * 
 * @author Luca Garulli
 * 
 */
public class OrientDBClient extends DB
{

    private ODatabaseDocumentTx db;
    private static final String CLASS = "usertable";
    private ODictionary<ORecordInternal<?>> dictionary;

    /**
     * Initialize any state for this DB. Called once per DB instance; there is
     * one DB instance per client thread.
     */
    public void init() throws DBException
    {
        // initialize OrientDB driver
        String url = Utils.mapGetDefault( getProperties(), "orientdb.url", "local:C:/temp/databases/ycsb" );
        String user = Utils.mapGetDefault( getProperties(), "orientdb.user", "admin" );
        String password = Utils.mapGetDefault( getProperties(), "orientdb.password", "admin" );
        Boolean newdb = Boolean.parseBoolean( Utils.mapGetDefault( getProperties(), "orientdb.newdb", "false" ) );

        try
        {
            System.out.println( "OrientDB loading database url = " + url );

            OGlobalConfiguration.STORAGE_KEEP_OPEN.setValue( false );
            db = new ODatabaseDocumentTx( url );
            if ( db.exists() )
            {
                db.open( user, password );
                if ( newdb )
                {
                    System.out.println( "OrientDB drop and recreate fresh db" );
                    db.drop();
                    db.create();
                }
            }
            else
            {
                System.out.println( "OrientDB database not found, create fresh db" );
                db.create();
            }

            System.out.println( "OrientDB connection created with " + url );

            dictionary = db.getMetadata().getIndexManager().getDictionary();
            if ( !db.getMetadata().getSchema().existsClass( CLASS ) )
                db.getMetadata().getSchema().createClass( CLASS );

            db.declareIntent( new OIntentMassiveInsert() );

        }
        catch ( Exception e1 )
        {
            System.err.println( "Could not initialize OrientDB connection pool for Loader: " + e1.toString() );
            e1.printStackTrace();
            return;
        }

    }

    @Override
    public void cleanup() throws DBException
    {
        if ( db != null )
        {
            db.close();
            db = null;
        }
    }

    /**
     * Insert a record in the database. Any field/value pairs in the specified
     * values HashMap will be written into the record with the specified record
     * key.
     * 
     * @param table The name of the table
     * @param key The record key of the record to insert.
     * @param values A HashMap of field/value pairs to insert in the record
     * @return Zero on success, a non-zero error code on error. See this class's
     *         description for a discussion of error codes.
     */
    @Override
    public int insert( String table, String key, Map<String, ByteIterator> values )
    {
        try
        {
            final ODocument document = new ODocument( CLASS );
            for ( Entry<String, String> entry : StringByteIterator.getStringMap( values ).entrySet() )
                document.field( entry.getKey(), entry.getValue() );
            document.save();
            dictionary.put( key, document );

            return 0;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    /**
     * Delete a record from the database.
     *
     * @param table The name of the table
     * @param key The record key of the record to delete.
     * @return Zero on success, a non-zero error code on error. See this class's description for a discussion of error codes.
     */
    public int delete( String table, String key )
    {
        try
        {
            dictionary.remove( key );
            return 0;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Read a record from the database. Each field/value pair from the result
     * will be stored in a HashMap.
     * 
     * @param table The name of the table
     * @param key The record key of the record to read.
     * @param fields The list of fields to read, or null for all of them
     * @param result A HashMap of field/value pairs for the result
     * @return Zero on success, a non-zero error code on error or "not found".
     */
    @Override
    public int read( String table, String key, Set<String> fields, Map<String, ByteIterator> result )
    {
        try
        {
            final ODocument document = dictionary.get( key );
            if ( document != null )
            {
                if ( fields != null )
                    for ( String field : fields )
                        result.put( field, new StringByteIterator( (String) document.field( field ) ) );
                else
                    for ( String field : document.fieldNames() )
                        result.put( field, new StringByteIterator( (String) document.field( field ) ) );
                return 0;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Update a record in the database. Any field/value pairs in the specified
     * values HashMap will be written into the record with the specified record
     * key, overwriting any existing values with the same field name.
     * 
     * @param table The name of the table
     * @param key The record key of the record to write.
     * @param values A HashMap of field/value pairs to update in the record
     * @return Zero on success, a non-zero error code on error. See this class's
     *         description for a discussion of error codes.
     */
    @Override
    public int update( String table, String key, Map<String, ByteIterator> values )
    {
        try
        {
            final ODocument document = dictionary.get( key );
            if ( document != null )
            {
                for ( Entry<String, String> entry : StringByteIterator.getStringMap( values ).entrySet() )
                    document.field( entry.getKey(), entry.getValue() );
                document.save();
                return 0;
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Perform a range scan for a set of records in the database. Each
     * field/value pair from the result will be stored in a HashMap.
     * 
     * @param table The name of the table
     * @param startkey The record key of the first record to read.
     * @param recordcount The number of records to read
     * @param fields The list of fields to read, or null for all of them
     * @param result A Vector of HashMaps, where each HashMap is a set
     *            field/value pairs for one record
     * @return Zero on success, a non-zero error code on error. See this class's
     *         description for a discussion of error codes.
     */
    @Override
    public int scan( String table, String startkey, int recordcount, Set<String> fields,
            Vector<Map<String, ByteIterator>> result )
    {
        try
        {
            final Collection<ODocument> documents = dictionary.getIndex().getEntriesMajor( startkey, true, recordcount );
            for ( ODocument document : documents )
            {
                final HashMap<String, ByteIterator> entry = new HashMap<String, ByteIterator>( fields.size() );
                result.add( entry );

                for ( String field : fields )
                    entry.put( field, new StringByteIterator( (String) document.field( field ) ) );
            }

            return 0;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return 1;
    }
}
