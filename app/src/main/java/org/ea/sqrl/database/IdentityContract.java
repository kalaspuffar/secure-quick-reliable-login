package org.ea.sqrl.database;

import android.provider.BaseColumns;

/**
 *
 * @author Daniel Persson
 */
public class IdentityContract {
    private IdentityContract() {}

    public static class IdentityEntry implements BaseColumns {
        public static final String TABLE_NAME = "identities";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DATA = "data";
    }
}
