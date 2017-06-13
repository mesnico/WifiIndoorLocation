package com.unipi.nicola.indoorlocator.fingerprinting;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nicola on 15/04/2017.
 */

public class WifiFingerprintDBAdapter {
    private Context context;
    private SQLiteDatabase database;
    private WifiFingerprintDBHelper dbHelper;

    public WifiFingerprintDBAdapter(Context context) {
        this.context = context;
    }

    public WifiFingerprintDBAdapter open(boolean writable) throws SQLException {
        dbHelper = new WifiFingerprintDBHelper(context);
        database = (writable) ? dbHelper.getWritableDatabase() : dbHelper.getReadableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public boolean isOpen(){
        return database.isOpen();
    }

    /**
     * This functions inserts a new computed fingerprint in the database.
     * @param fp The fingerprint to be inserted in the database
     * @throws SQLException
     */
    public void insertFingerprint(WifiFingerprint fp) throws SQLException{
        //This is a transaction since I need to perform multiple insertions
        database.beginTransaction();

        try {
            //First, the fingerprint entry is added (the id is automatically generated)
            ContentValues fpInsertion = new ContentValues();
            fpInsertion.put(WifiFingerprintDBHelper.COLUMN_LATITUDE, fp.getLocation().getLatitude());
            fpInsertion.put(WifiFingerprintDBHelper.COLUMN_LONGITUDE, fp.getLocation().getLongitude());
            fpInsertion.put(WifiFingerprintDBHelper.COLUMN_ALTITUDE, fp.getLocation().getAltitude());
            fpInsertion.put(WifiFingerprintDBHelper.COLUMN_FPLABEL, fp.getLocationLabel());
            long insertedId = database.insertOrThrow(
                    WifiFingerprintDBHelper.TABLE_FINGERPRINTS, null, fpInsertion);

            //Second, the access point list is added
            for (AccessPointInfos api : fp.getAccessPoints()) {
                ContentValues apsInsertion = new ContentValues();
                apsInsertion.put(WifiFingerprintDBHelper.COLUMN_FPID, insertedId);
                apsInsertion.put(WifiFingerprintDBHelper.COLUMN_HWADDR, api.getHwAddress());
                apsInsertion.put(WifiFingerprintDBHelper.COLUMN_SIGNALSTRENGTH, api.getSignalStrength());
                database.insertOrThrow(
                        WifiFingerprintDBHelper.TABLE_ACCESS_POINTS, null, apsInsertion);
            }

            //If arrived here without errors, then the transaction is successful
            database.setTransactionSuccessful();
        } catch(SQLException e){
            throw e;
        } finally {
            database.endTransaction();
        }
    }

    /**
     * This function searches the DB for fingerprints having common access points between the query
     * and the DB.
     * The fingerprints having a number of intersecting access points between 'lowerbound' and the
     * number of access points in the query fingerprints are returned.
     * @param fp The query fingerprint
     * @param lowerbound The lowerbound for the cardinality of the intersection
     * @return The list of fingerprints satisfying the intersection constraints
     * @throws SQLException
     */
    //@TargetApi(24)
    public List<WifiFingerprint> extractCommonFingerprints(WifiFingerprint fp, int lowerbound) throws SQLException {
        //The string formatted list of HWAddress from the input fingerprint access points is created
        /*String[] apHwAddresses = (fp.getAccessPoints()
                .stream()
                .map(AccessPointInfos::getHwAddress)
                .collect(Collectors.toList()))
                .toArray();*/
        int i = 0;
        String[] apHwAddresses = new String[fp.getAccessPoints().size()];
        for (AccessPointInfos api : fp.getAccessPoints()) {
            apHwAddresses[i] = api.getHwAddress();
            i++;
        }
        String listOfInputAPs = TextUtils.join("', '", apHwAddresses);
        listOfInputAPs = "('" + listOfInputAPs + "')";

        final String searchQuery =
                "SELECT * FROM " + WifiFingerprintDBHelper.TABLE_FINGERPRINTS + " NATURAL JOIN " + WifiFingerprintDBHelper.TABLE_ACCESS_POINTS +
                        " WHERE " + WifiFingerprintDBHelper.TABLE_FINGERPRINTS + "." + WifiFingerprintDBHelper.COLUMN_FPID + " IN (" +
                        " SELECT " + WifiFingerprintDBHelper.COLUMN_FPID + " FROM " + WifiFingerprintDBHelper.TABLE_ACCESS_POINTS +
                        " WHERE " + WifiFingerprintDBHelper.COLUMN_HWADDR + " IN " + listOfInputAPs +
                        " GROUP BY " + WifiFingerprintDBHelper.COLUMN_FPID +
                        " HAVING COUNT(*) BETWEEN " + lowerbound + " AND " + fp.getAccessPoints().size() + ")";

        Cursor searchResultCursor = database.rawQuery(searchQuery, null);
        List<WifiFingerprint> l = buildFingerprintList(searchResultCursor);
        searchResultCursor.close();
        return l;
    }

    /**
     * This function searches the DB for all fingerprints having the same IAP as the fingerprint
     * passed in input. 'iapNumber' of the input fingerprint (IAP set of that fingerprint) must match
     * the APs in the DB for
     * a certain fingerprint in the DB. Note: it is not true the contrary: in fact, not all the APs
     * from the found FP in the DB must match the 'iapNumber' APs in the input FP.
     * @param fp The fingerprint containing the important access points for the current search
     * @param iapNumber The number of IAP considered for this particular Fingerprint search
     * @return A list of fingerprints having the same IAP of the input fingerprint
     * @throws SQLException
     */
    List<WifiFingerprint> extractIAPFingerprints(WifiFingerprint fp, int iapNumber) throws SQLException{
        return null;
    }

    private List<WifiFingerprint> buildFingerprintList (Cursor searchResultCursor){
        int currentFingerprintID = -1;
        int oldFingerprintID = -1;
        List<WifiFingerprint> fpList = new ArrayList<>();
        List<AccessPointInfos> apList = new ArrayList<>();

        String locLabel = "";
        Location loc = null;
        boolean firstIt = true;
        boolean lastIt = false;

        while(searchResultCursor.moveToNext() || lastIt){
            //get the value of the current fingerprint
            if(!lastIt) {
                currentFingerprintID = searchResultCursor.getInt(
                        searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_FPID));
            }

            //executed whenever edgecase is true (limits of the result set) or if I am scanning
            //different APs from the same FP.
            if ((firstIt || currentFingerprintID == oldFingerprintID) && !lastIt) {
                //insert the AP in the same fingerprint object
                AccessPointInfos currentAP = new AccessPointInfos(
                        searchResultCursor.getInt(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_SIGNALSTRENGTH)),
                        searchResultCursor.getString(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_HWADDR)),
                        null
                );
                //get the current AP and the location from that row of the DB
                apList.add(currentAP);
                loc = new Location("WifiFingerprint");
                loc.setLatitude(searchResultCursor.getDouble(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_LATITUDE)));
                loc.setLongitude(searchResultCursor.getDouble(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_LONGITUDE)));
                loc.setAltitude(searchResultCursor.getDouble(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_ALTITUDE)));
                locLabel = searchResultCursor.getString(searchResultCursor.getColumnIndex(WifiFingerprintDBHelper.COLUMN_FPLABEL));
                firstIt = false;
            } else {
                //we have to add the constructed fingerprint to the list
                fpList.add(new WifiFingerprint(apList, loc, locLabel));
                apList = new ArrayList<>();
                //after having flushed also the last values, exit from the cycle
                if(lastIt) break;
                searchResultCursor.moveToPrevious();
            }

            if(searchResultCursor.isLast()){
                //set the lastIt to true so that also the last row can be pushed to the list
                lastIt = true;
            }
            oldFingerprintID = currentFingerprintID;
        }
        return fpList;
    }
}
